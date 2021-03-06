package scala.tools.nsc
package interactive

import java.io.{ PrintWriter, StringWriter }

import scala.collection.mutable.{LinkedHashMap, SynchronizedMap}
import scala.concurrent.SyncVar
import scala.util.control.ControlException
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.{SourceFile, Position, RangePosition, OffsetPosition, NoPosition, WorkScheduler}
import scala.tools.nsc.reporters._
import scala.tools.nsc.symtab._
import scala.tools.nsc.ast._

/** The main class of the presentation compiler in an interactive environment such as an IDE
 */
class Global(settings: Settings, reporter: Reporter) 
  extends scala.tools.nsc.Global(settings, reporter) 
     with CompilerControl 
     with RangePositions
     with ContextTrees 
     with RichCompilationUnits { 
self =>

  import definitions._

  final val debugIDE = false

  override def onlyPresentation = true

  /** A list indicating in which order some units should be typechecked.
   *  All units in firsts are typechecked before any unit not in this list
   *  Modified by askToDoFirst, reload, typeAtTree.
   */
  var firsts: List[SourceFile] = List()

  /** A map of all loaded files to the rich compilation units that correspond to them.
   */ 
  val unitOfFile = new LinkedHashMap[AbstractFile, RichCompilationUnit] with
                       SynchronizedMap[AbstractFile, RichCompilationUnit]

  /** The currently active typer run */
  private var currentTyperRun: TyperRun = _

  /** Is a background compiler run needed? */
  private var outOfDate = false

  /** Units compiled by a run with id >= minRunId are considered up-to-date  */
  private[interactive] var minRunId = 1

  /** Is a reload/background compiler currently running? */
  private var acting = false

  // ----------- Overriding hooks in nsc.Global -----------------------
  
  /** Called from typechecker, which signal hereby that a node has been completely typechecked.
   *  If the node is included in unit.targetPos, abandons run and returns newly attributed tree.
   *  Otherwise, if there's some higher priority work to be done, also abandons run with a FreshRunReq.
   *  @param  context  The context that typechecked the node
   *  @param  old      The original node
   *  @param  result   The transformed node
   */
  override def signalDone(context: Context, old: Tree, result: Tree) {
    def integrateNew() {
      context.unit.body = new TreeReplacer(old, result) transform context.unit.body
    }
    if (activeLocks == 0) {
      if (context.unit != null && 
          result.pos.isOpaqueRange && 
          (result.pos includes context.unit.targetPos)) {
        integrateNew()
        var located = new Locator(context.unit.targetPos) locateIn result
        if (located == EmptyTree) {
          println("something's wrong: no "+context.unit+" in "+result+result.pos)
          located = result
        }
        throw new TyperResult(located)
      }
      val typerRun = currentTyperRun
      
      while(true) 
        try {
          pollForWork()
          if (typerRun == currentTyperRun)
            return
          
          // @Martin
          // Guard against NPEs in integrateNew if context.unit == null here.
          // But why are we doing this at all? If it was non-null previously
          // integrateNew will already have been called. If it was null previously
          // it will still be null now?
          if (context.unit != null)          
            integrateNew()
          throw new FreshRunReq
        } catch {
          case ex : ValidateError => // Ignore, this will have been reported elsewhere
          case t : Throwable => throw t
        }
    }
  }

  /** Called from typechecker every time a context is created.
   *  Registers the context in a context tree
   */
  override def registerContext(c: Context) = c.unit match {
    case u: RichCompilationUnit => addContext(u.contexts, c)
    case _ =>
  }

  // ----------------- Polling ---------------------------------------

  /** Called from runner thread and signalDone:
   *  Poll for exeptions. 
   *  Poll for work reload/typedTreeAt/doFirst commands during background checking.
   */
  def pollForWork() {
    scheduler.pollException() match {
      case Some(ex: CancelActionReq) => if (acting) throw ex
      case Some(ex: FreshRunReq) => 
        currentTyperRun = new TyperRun()
        minRunId = currentRunId
        if (outOfDate) throw ex 
        else outOfDate = true
      case Some(ex: Throwable) => throw ex
      case _ =>
    }
    scheduler.nextWorkItem() match {
      case Some(action) =>
        try {
          acting = true
          if (debugIDE) println("picked up work item: "+action)
          action()
          if (debugIDE) println("done with work item: "+action)
        } catch {
          case ex: CancelActionReq =>
            if (debugIDE) println("cancelled work item: "+action)
        } finally {
          if (debugIDE) println("quitting work item: "+action)
          acting = false
        }
      case None =>
    }
  }    

  def debugInfo(source : SourceFile, start : Int, length : Int): String = {
    println("DEBUG INFO "+source+"/"+start+"/"+length)
    val end = start+length
    val pos = rangePos(source, start, start, end)

    val tree = locateTree(pos)
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    treePrinters.create(pw).print(tree)
    pw.flush
    
    val typed = new Response[Tree]
    askTypeAt(pos, typed)
    val typ = typed.get.left.toOption match {
      case Some(tree) =>
        val sw = new StringWriter
        val pw = new PrintWriter(sw)
        treePrinters.create(pw).print(tree)
        pw.flush
        sw.toString
      case None => "<None>"      
    }

    val completionResponse = new Response[List[Member]]
    askTypeCompletion(pos, completionResponse)
    val completion = completionResponse.get.left.toOption match {
      case Some(members) =>
        members mkString "\n"
      case None => "<None>"      
    }
    
    source.content.view.drop(start).take(length).mkString+" : "+source.path+" ("+start+", "+end+
    ")\n\nlocateTree:\n"+sw.toString+"\n\naskTypeAt:\n"+typ+"\n\ncompletion:\n"+completion
  }

  // ----------------- The Background Runner Thread -----------------------

  /** The current presentation compiler runner */
  private var compileRunner = newRunnerThread

  /** Create a new presentation compiler runner.
   */
  def newRunnerThread: Thread = new Thread("Scala Presentation Compiler") {
    override def run() {
      try {
        while (true) {
          scheduler.waitForMoreWork()
          pollForWork()
          while (outOfDate) {
            try {
              backgroundCompile()
              outOfDate = false
            } catch {
              case ex: FreshRunReq => 
            }
          }
        }
      } catch {
        case ex: ShutdownReq => 
          ;
        case ex => 
          outOfDate = false
          compileRunner = newRunnerThread
          ex match {
            case _ : FreshRunReq =>   // This shouldn't be reported
            case _ : ValidateError => // This will have been reported elsewhere
            case _ => ex.printStackTrace(); inform("Fatal Error: "+ex)
          }
      }
    }
    start()
  }

  /** Compile all given units
   */ 
  private def backgroundCompile() {
    if (debugIDE) inform("Starting new presentation compiler type checking pass")
    reporter.reset
    firsts = firsts filter (s => unitOfFile contains (s.file))
    val prefix = firsts map unitOf
    val units = prefix ::: (unitOfFile.valuesIterator.toList diff prefix) filter (!_.isUpToDate)
    recompile(units)
    if (debugIDE) inform("Everything is now up to date")
  }

  /** Reset unit to just-parsed state */
  def reset(unit: RichCompilationUnit): Unit =
    if (unit.status > JustParsed) {
      unit.depends.clear()
      unit.defined.clear()
      unit.synthetics.clear()
      unit.toCheck.clear()
      unit.targetPos = NoPosition
      unit.contexts.clear()
      unit.body = EmptyTree
      unit.status = NotLoaded
    }

  /** Parse unit and create a name index. */
  def parse(unit: RichCompilationUnit): Unit = {
    currentTyperRun.compileLate(unit)
    if (!reporter.hasErrors) validatePositions(unit.body)
    //println("parsed: [["+unit.body+"]]")
    unit.status = JustParsed
  }

  /** Make sure symbol and type attributes are reset and recompile units. 
   */
  def recompile(units: List[RichCompilationUnit]) {
    for (unit <- units) {
      reset(unit)
      if (debugIDE) inform("parsing: "+unit)
      parse(unit)
    }
    for (unit <- units) {
      if (debugIDE) inform("type checking: "+unit)
      activeLocks = 0
      currentTyperRun.typeCheck(unit)
      unit.status = currentRunId
    }
  }

  /** Move list of files to front of firsts */
  def moveToFront(fs: List[SourceFile]) {
    firsts = fs ::: (firsts diff fs)
  }

  // ----------------- Implementations of client commmands -----------------------
  
  def respond[T](result: Response[T])(op: => T): Unit =
    try {
      result set Left(op)
      return
    } catch {
      case ex : FreshRunReq =>
        scheduler.postWorkItem(() => respond(result)(op))
        throw ex
      case ex =>
        result set Right(ex)
        throw ex
    }

  /** Make sure a set of compilation units is loaded and parsed */
  def reloadSources(sources: List[SourceFile]) {
    currentTyperRun = new TyperRun()
    for (source <- sources) {
      val unit = new RichCompilationUnit(source)
      unitOfFile(source.file) = unit
      parse(unit)
    }
    moveToFront(sources)
  }

  /** Make sure a set of compilation units is loaded and parsed */
  def reload(sources: List[SourceFile], result: Response[Unit]) {
    respond(result)(reloadSources(sources))
    if (outOfDate) throw new FreshRunReq
    else outOfDate = true
  }

  /** A fully attributed tree located at position `pos`  */
  def typedTreeAt(pos: Position): Tree = {
    val unit = unitOf(pos)
    val sources = List(unit.source)
    if (unit.status == NotLoaded) reloadSources(sources)
    moveToFront(sources)
    val typedTree = currentTyperRun.typedTreeAt(pos)
    new Locator(pos) locateIn typedTree
  }

  /** A fully attributed tree corresponding to the entire compilation unit  */
  def typedTree(source: SourceFile, forceReload: Boolean): Tree = {
    val unit = unitOf(source)
    val sources = List(source)
    if (unit.status == NotLoaded || forceReload) reloadSources(sources)
    moveToFront(sources)
    currentTyperRun.typedTree(unitOf(source))
  }

  /** Set sync var `result` to a fully attributed tree located at position `pos`  */
  def getTypedTreeAt(pos: Position, result: Response[Tree]) {
    respond(result)(typedTreeAt(pos))
  }

  /** Set sync var `result` to a fully attributed tree corresponding to the entire compilation unit  */
  def getTypedTree(source : SourceFile, forceReload: Boolean, result: Response[Tree]) {
    respond(result)(typedTree(source, forceReload))
  }

  def stabilizedType(tree: Tree): Type = tree match {
    case Ident(_) if tree.symbol.isStable => singleType(NoPrefix, tree.symbol)
    case Select(qual, _) if tree.symbol.isStable => singleType(qual.tpe, tree.symbol)
    case Import(expr, selectors) =>
      tree.symbol.info match {
        case analyzer.ImportType(expr) => expr match {
          case s@Select(qual, name) => singleType(qual.tpe, s.symbol)
          case i : Ident => i.tpe
          case _ => tree.tpe
        }
        case _ => tree.tpe
      }
    
    case _ => tree.tpe
  }

  import analyzer.{SearchResult, ImplicitSearch}

  def getScopeCompletion(pos: Position, result: Response[List[Member]]) {
    respond(result) { scopeMembers(pos) }
  }

  val Dollar = newTermName("$")

  /** Return all members visible without prefix in context enclosing `pos`. */
  def scopeMembers(pos: Position): List[ScopeMember] = {
    typedTreeAt(pos) // to make sure context is entered
    val context = doLocateContext(pos)
    val locals = new LinkedHashMap[Name, ScopeMember]
    def addScopeMember(sym: Symbol, pre: Type, viaImport: Tree) =
      if (!sym.name.decode.containsName(Dollar) &&  
          !sym.hasFlag(Flags.SYNTHETIC) &&
          !locals.contains(sym.name)) {
        //println("adding scope member: "+pre+" "+sym)
        locals(sym.name) = new ScopeMember(
          sym, 
          pre.memberType(sym), 
          context.isAccessible(sym, pre, false),
          viaImport)
      }
    var cx = context
    while (cx != NoContext) {
      for (sym <- cx.scope)
        addScopeMember(sym, NoPrefix, EmptyTree)
      cx = cx.enclClass
      val pre = cx.prefix
      for (sym <- pre.members) 
        addScopeMember(sym, pre, EmptyTree)
      cx = cx.outer
    }
    for (imp <- context.imports) {
      val pre = imp.qual.tpe
      for (sym <- imp.allImportedSymbols) {
        addScopeMember(sym, pre, imp.qual)
      }
    }
    val result = locals.valuesIterator.toList
    if (debugIDE) for (m <- result) println(m)
    result
  }

  def getTypeCompletion(pos: Position, result: Response[List[Member]]) {
    respond(result) { typeMembers(pos) }
    if (debugIDE) scopeMembers(pos)
  }

  def typeMembers(pos: Position): List[TypeMember] = {
    val tree1 = typedTreeAt(pos)
    val tree0 = tree1 match {
      case tt : TypeTree => tt.original
      case t => t 
    }
    val tree = tree0 match {
      case s@Select(qual, name) if s.tpe == ErrorType => qual
      case t => t
    }
    
    println("typeMembers at "+tree+" "+tree.tpe)
    val context = doLocateContext(pos)
    val superAccess = tree.isInstanceOf[Super]
    val scope = new Scope
    val members = new LinkedHashMap[Symbol, TypeMember]
    def addTypeMember(sym: Symbol, pre: Type, inherited: Boolean, viaView: Symbol) {
      val symtpe = pre.memberType(sym)
      if (scope.lookupAll(sym.name) forall (sym => !(members(sym).tpe matches symtpe))) {
        scope enter sym
        members(sym) = new TypeMember(
          sym,
          symtpe,
          context.isAccessible(sym, pre, superAccess && (viaView == NoSymbol)),
          inherited,
          viaView)
      }
    }        
    def viewApply(view: SearchResult): Tree = {
      assert(view.tree != EmptyTree)
      try {
        analyzer.newTyper(context.makeImplicit(false)).typed(Apply(view.tree, List(tree)) setPos tree.pos)
      } catch {
        case ex: TypeError => EmptyTree
      }
    }
    val pre = stabilizedType(tree)
    val ownerTpe = if (tree.tpe != null) tree.tpe else pre
    for (sym <- ownerTpe.decls)
      addTypeMember(sym, pre, false, NoSymbol)
    for (sym <- ownerTpe.members)
      addTypeMember(sym, pre, true, NoSymbol)
    val applicableViews: List[SearchResult] = 
      new ImplicitSearch(tree, functionType(List(ownerTpe), AnyClass.tpe), true, context.makeImplicit(false))
        .allImplicits
    for (view <- applicableViews) {
      val vtree = viewApply(view)
      val vpre = stabilizedType(vtree)
      for (sym <- vtree.tpe.members) {
        addTypeMember(sym, vpre, false, view.tree.symbol)
      }
    }
    members.valuesIterator.toList
  }

  // ---------------- Helper classes ---------------------------

  /** A transformer that replaces tree `from` with tree `to` in a given tree */
  class TreeReplacer(from: Tree, to: Tree) extends Transformer {
    override def transform(t: Tree): Tree = {
      if (t == from) to
      else if ((t.pos includes from.pos) || t.pos.isTransparent) super.transform(t)
      else t
    }
  }

  /** The typer run */
  class TyperRun extends Run {
    // units is always empty
    // symSource, symData are ignored
    override def compiles(sym: Symbol) = false

    def typeCheck(unit: CompilationUnit): Unit = applyPhase(typerPhase, unit)

    def enterNames(unit: CompilationUnit): Unit = applyPhase(namerPhase, unit)

    /** Return fully attributed tree at given position
     *  (i.e. largest tree that's contained by position)
     */
    def typedTreeAt(pos: Position): Tree = {
      println("starting typedTreeAt")
      val tree = locateTree(pos)
      println("at pos "+pos+" was found: "+tree+tree.pos.show)
      if (tree.tpe ne null) {
        println("already attributed")
        tree
      } else {
        val unit = unitOf(pos)
        assert(unit.status >= JustParsed)
        unit.targetPos = pos
        try {
          println("starting targeted type check")
          typeCheck(unit)
          throw new FatalError("tree not found")
        } catch {
          case ex: TyperResult => 
            ex.tree
        } finally {
          unit.targetPos = NoPosition
        }
      }
    } 

    def typedTree(unit: RichCompilationUnit): Tree = {
      assert(unit.status >= JustParsed)
      unit.targetPos = NoPosition
      typeCheck(unit)
      unit.body
    } 

    /** Apply a phase to a compilation unit
     *  @return true iff typechecked correctly
     */
    private def applyPhase(phase: Phase, unit: CompilationUnit) {
      val oldSource = reporter.getSource          
      reporter.withSource(unit.source) {
        atPhase(phase) { phase.asInstanceOf[GlobalPhase] applyPhase unit }
      }
    }
  }

  class TyperResult(val tree: Tree) extends Exception with ControlException
  
  assert(globalPhase.id == 0)
}
