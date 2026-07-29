package com.hardwareai.support.conversation;
import com.hardwareai.support.knowledge.ObjectStorage;
import com.hardwareai.support.knowledge.OcrClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Asynchronous OCR candidate extraction; results never alter product context without explicit user confirmation. */
@Component
class AttachmentProcessingWorker {
 private final AttachmentProcessingJobRepository jobs; private final MessageAttachmentRepository attachments; private final AttachmentAnalysisRepository analyses; private final ObjectStorage storage; private final OcrClient ocr;
 AttachmentProcessingWorker(AttachmentProcessingJobRepository jobs, MessageAttachmentRepository attachments, AttachmentAnalysisRepository analyses, ObjectStorage storage, OcrClient ocr){this.jobs=jobs;this.attachments=attachments;this.analyses=analyses;this.storage=storage;this.ocr=ocr;}
 @Scheduled(fixedDelayString="${app.worker-delay-ms:2000}") void process(){jobs.findFirstByStatusOrderByCreatedAt("PENDING").ifPresent(job->{try{job.start();jobs.save(job);var attachment=attachments.findById(job.attachmentId()).orElseThrow();var result=ocr.extract(attachment.contentType(),storage.get(attachment.objectKey()));analyses.save(new AttachmentAnalysis(attachment.id(),result.text(),result.confidence()));job.complete();jobs.save(job);}catch(Exception e){job.fail(e);jobs.save(job);}});}
}
