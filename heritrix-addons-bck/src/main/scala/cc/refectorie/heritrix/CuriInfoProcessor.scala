package cc.refectorie.heritrix

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.archive.io.RecordingInputStream;
import org.archive.io.ReplayInputStream;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.archive.net.UURI;
import org.archive.util.FileUtils;


class UberInfoWriterProcessor extends Processor {
  val log = Logger.getLogger(classOf[UberInfoWriterProcessor].getName)

  // override def shouldProcess(curi: CrawlURI):Boolean = isSuccess(curi)
  override def shouldProcess(curi: CrawlURI):Boolean = true

  override def innerProcess(curi: CrawlURI):Unit = {
    printUberInfo(curi)
  }


  // initialTasks() = pre-crawl hook
  // finalTasks() = post-crawl hook
  // report()

  def printUberInfo(curi:CrawlURI):Unit = {
    log.info("getOutCandidates()                 " + curi.getOutCandidates())
    log.info("getOutLinks()                      " + curi.getOutLinks())
    log.info("getAnnotations()                   " + curi.getAnnotations())
    log.info("getNonFatalFailures()              " + curi.getNonFatalFailures())
    log.info("getFullVia()                       " + curi.getFullVia())
    log.info("getPrerequisiteUri()               " + curi.getPrerequisiteUri())
    log.info("getFetchType()                     " + curi.getFetchType())
    log.info("getHttpMethod()                    " + curi.getHttpMethod())
    log.info("getViaContext()                    " + curi.getViaContext())
    log.info("getOverlayNames()                  " + curi.getOverlayNames())
    // log.info("getOverlayMap(String name)         " + curi.getOverlayMap(String name))
    // log.info("singleLineReportData()             " + curi.singleLineReportData())
    log.info("getData()                          " + curi.getData())
    log.info("getPersistentDataMap()             " + curi.getPersistentDataMap())
    // log.info("getHolder()                        " + curi.getHolder())
    // log.info("getHolderKey()                     " + curi.getHolderKey())
    // log.info("getRecorder()                      " + curi.getRecorder())
    // log.info("getCredentialAvatars()             " + curi.getCredentialAvatars())
    log.info("flattenVia()                       " + curi.flattenVia())
    log.info("getCanonicalString()               " + curi.getCanonicalString())
    //log.info("getClassKey()                      " + curi.getClassKey())
    //log.info("getContentDigestSchemeString()     " + curi.getContentDigestSchemeString())
    //log.info("getContentDigestString()           " + curi.getContentDigestString())
    log.info("getContentType()                   " + curi.getContentType())
    log.info("getCrawlURIString()                " + curi.getCrawlURIString())
    //log.info("getDNSServerIPLabel()              " + curi.getDNSServerIPLabel())
    //log.info("getPathFromSeed()                  " + curi.getPathFromSeed())
    //log.info("getSourceTag()                     " + curi.getSourceTag())
    //log.info("getURI()                           " + curi.getURI())
    //log.info("getUserAgent()                     " + curi.getUserAgent())
    //log.info("singleLineLegend()                 " + curi.singleLineLegend())
    //log.info("singleLineReport()                 " + curi.singleLineReport())
    //log.info("getReports()                       " + curi.getReports())
    //log.info("getBaseURI()                       " + curi.getBaseURI())
    //log.info("getPolicyBasisUURI()               " + curi.getPolicyBasisUURI())
    //log.info("getUURI()                          " + curi.getUURI())
    log.info("getVia()                           " + curi.getVia())
    // log.info("containsDataKey(String key)        " + curi.containsDataKey(String key))
    log.info("forceFetch()                       " + curi.forceFetch())
    //log.info("hasBeenLinkExtracted()             " + curi.hasBeenLinkExtracted())
    //log.info("hasCredentialAvatars()             " + curi.hasCredentialAvatars())
    //log.info("hasPrerequisiteUri()               " + curi.hasPrerequisiteUri())
    //log.info("haveOverlayNamesBeenSet()          " + curi.haveOverlayNamesBeenSet())
    //log.info("is2XXSuccess()                     " + curi.is2XXSuccess())
    //log.info("isHttpTransaction()                " + curi.isHttpTransaction())
    //log.info("isLocation()                       " + curi.isLocation())
    //log.info("isPrerequisite()                   " + curi.isPrerequisite())
    //log.info("isSeed()                           " + curi.isSeed())
    //log.info("isSuccess()                        " + curi.isSuccess())
    //log.info("getContentDigest()                 " + curi.getContentDigest())
    //log.info("getDeferrals()                     " + curi.getDeferrals())
    //log.info("getEmbedHopCount()                 " + curi.getEmbedHopCount())
    //log.info("getFetchAttempts()                 " + curi.getFetchAttempts())
    //log.info("getFetchStatus()                   " + curi.getFetchStatus())
    //log.info("getHolderCost()                    " + curi.getHolderCost())
    //log.info("getLinkHopCount()                  " + curi.getLinkHopCount())
    //log.info("getPrecedence()                    " + curi.getPrecedence())
    //log.info("getSchedulingDirective()           " + curi.getSchedulingDirective())
    //log.info("getThreadNumber()                  " + curi.getThreadNumber())
    //log.info("getTransHops()                     " + curi.getTransHops())
    //log.info("incrementFetchAttempts()           " + curi.incrementFetchAttempts())
    //log.info("getContentLength()                 " + curi.getContentLength())
    //log.info("getContentSize()                   " + curi.getContentSize())
    //log.info("getFetchBeginTime()                " + curi.getFetchBeginTime())
    //log.info("getFetchCompletedTime()            " + curi.getFetchCompletedTime())
    //log.info("getFetchDuration()                 " + curi.getFetchDuration())
    //log.info("getOrdinal()                       " + curi.getOrdinal())
    //log.info("getPolitenessDelay()               " + curi.getPolitenessDelay())
    //log.info("getRecordedSize()                  " + curi.getRecordedSize())
    //log.info("getRescheduleTime()                " + curi.getRescheduleTime())
  }
}


// Collection<CrawlURI>   getOutCandidates()
// Collection<Link>       getOutLinks()
// Collection<String>     getAnnotations()
// Collection<Throwable>  getNonFatalFailures()
// CrawlURI               getFullVia()
// CrawlURI               getPrerequisiteUri()
// FetchType              getFetchType()
// HttpMethod             getHttpMethod()
// LinkContext            getViaContext()
// LinkedList<String>     getOverlayNames()
// Map<String,            Object> getOverlayMap(String name)
// Map<String,            Object> singleLineReportData()
// Map<String,Object>     getData()
// Map<String,Object>     getPersistentDataMap()
// Object                 getHolder()
// Object                 getHolderKey()
// Recorder               getRecorder()
// Set<CredentialAvatar>  getCredentialAvatars()
// String                 flattenVia()
// String                 getCanonicalString()
// String                 getClassKey()
// String                 getContentDigestSchemeString()
// String                 getContentDigestString()
// String                 getContentType()
// String                 getCrawlURIString()
// String                 getDNSServerIPLabel()
// String                 getPathFromSeed()
// String                 getSourceTag()
// String                 getURI()
// String                 getUserAgent()
// String                 singleLineLegend()
// String                 singleLineReport()
// String[]               getReports()
// UURI                   getBaseURI()
// UURI                   getPolicyBasisUURI()
// UURI                   getUURI()
// UURI                   getVia()
// boolean                containsDataKey(String key)
// boolean                forceFetch()
// boolean                hasBeenLinkExtracted()
// boolean                hasCredentialAvatars()
// boolean                hasPrerequisiteUri()
// boolean                haveOverlayNamesBeenSet()
// boolean                is2XXSuccess()
// boolean                isHttpTransaction()
// boolean                isLocation()
// boolean                isPrerequisite()
// boolean                isSeed()
// boolean                isSuccess()
// byte[]                 getContentDigest()
// int                    getDeferrals()
// int                    getEmbedHopCount()
// int                    getFetchAttempts()
// int                    getFetchStatus()
// int                    getHolderCost()
// int                    getLinkHopCount()
// int                    getPrecedence()
// int                    getSchedulingDirective()
// int                    getThreadNumber()
// int                    getTransHops()
// int                    incrementFetchAttempts()
// long                   getContentLength()
// long                   getContentSize()
// long                   getFetchBeginTime()
// long                   getFetchCompletedTime()
// long                   getFetchDuration()
// long                   getOrdinal()
// long                   getPolitenessDelay()
// long                   getRecordedSize()
// long                   getRescheduleTime()






