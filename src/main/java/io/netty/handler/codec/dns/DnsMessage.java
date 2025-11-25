package io.netty.handler.codec.dns;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class DnsMessage extends AbstractReferenceCounted {
   private List<DnsQuestion> questions;
   private List<DnsResource> answers;
   private List<DnsResource> authority;
   private List<DnsResource> additional;
   private final DnsHeader header;

   DnsMessage(int id) {
      this.header = this.newHeader(id);
   }

   public DnsHeader header() {
      return this.header;
   }

   public List<DnsQuestion> questions() {
      return this.questions == null ? Collections.emptyList() : Collections.unmodifiableList(this.questions);
   }

   public List<DnsResource> answers() {
      return this.answers == null ? Collections.emptyList() : Collections.unmodifiableList(this.answers);
   }

   public List<DnsResource> authorityResources() {
      return this.authority == null ? Collections.emptyList() : Collections.unmodifiableList(this.authority);
   }

   public List<DnsResource> additionalResources() {
      return this.additional == null ? Collections.emptyList() : Collections.unmodifiableList(this.additional);
   }

   public DnsMessage addAnswer(DnsResource answer) {
      if (this.answers == null) {
         this.answers = new LinkedList();
      }

      this.answers.add(answer);
      return this;
   }

   public DnsMessage addQuestion(DnsQuestion question) {
      if (this.questions == null) {
         this.questions = new LinkedList();
      }

      this.questions.add(question);
      return this;
   }

   public DnsMessage addAuthorityResource(DnsResource resource) {
      if (this.authority == null) {
         this.authority = new LinkedList();
      }

      this.authority.add(resource);
      return this;
   }

   public DnsMessage addAdditionalResource(DnsResource resource) {
      if (this.additional == null) {
         this.additional = new LinkedList();
      }

      this.additional.add(resource);
      return this;
   }

   protected void deallocate() {
   }

   public boolean release() {
      release(this.questions());
      release(this.answers());
      release(this.additionalResources());
      release(this.authorityResources());
      return super.release();
   }

   private static void release(List<?> resources) {
      Iterator i$ = resources.iterator();

      while(i$.hasNext()) {
         Object resource = i$.next();
         ReferenceCountUtil.release(resource);
      }

   }

   public boolean release(int decrement) {
      release(this.questions(), decrement);
      release(this.answers(), decrement);
      release(this.additionalResources(), decrement);
      release(this.authorityResources(), decrement);
      return super.release(decrement);
   }

   private static void release(List<?> resources, int decrement) {
      Iterator i$ = resources.iterator();

      while(i$.hasNext()) {
         Object resource = i$.next();
         ReferenceCountUtil.release(resource, decrement);
      }

   }

   public DnsMessage touch(Object hint) {
      touch(this.questions(), hint);
      touch(this.answers(), hint);
      touch(this.additionalResources(), hint);
      touch(this.authorityResources(), hint);
      return this;
   }

   private static void touch(List<?> resources, Object hint) {
      Iterator i$ = resources.iterator();

      while(i$.hasNext()) {
         Object resource = i$.next();
         ReferenceCountUtil.touch(resource, hint);
      }

   }

   public DnsMessage retain() {
      retain(this.questions());
      retain(this.answers());
      retain(this.additionalResources());
      retain(this.authorityResources());
      super.retain();
      return this;
   }

   private static void retain(List<?> resources) {
      Iterator i$ = resources.iterator();

      while(i$.hasNext()) {
         Object resource = i$.next();
         ReferenceCountUtil.retain(resource);
      }

   }

   public DnsMessage retain(int increment) {
      retain(this.questions(), increment);
      retain(this.answers(), increment);
      retain(this.additionalResources(), increment);
      retain(this.authorityResources(), increment);
      super.retain(increment);
      return this;
   }

   private static void retain(List<?> resources, int increment) {
      Iterator i$ = resources.iterator();

      while(i$.hasNext()) {
         Object resource = i$.next();
         ReferenceCountUtil.retain(resource, increment);
      }

   }

   public DnsMessage touch() {
      super.touch();
      return this;
   }

   protected abstract DnsHeader newHeader(int var1);
}
