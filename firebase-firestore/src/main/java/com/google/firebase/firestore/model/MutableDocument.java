// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.firestore.model;

import androidx.annotation.NonNull;
import com.google.firestore.v1.Value;

public final class MutableDocument implements Document, Cloneable {

  private enum DocumentType {
    /**
     * Represents the initial state of a MutableDocument when only the document key is known.
     * Invalid documents transition to other states as mutations are applied. If a document remain
     * invalids after applying mutations, it should be discarded.
     */
    INVALID,
    /**
     * Represents a document in Firestore with a key, version, data and whether the data has local
     * mutations applied to it.
     */
    FOUND_DOCUMENT,
    /** Represents that no documents exists for the key at the given version. */
    NO_DOCUMENT,
    /**
     * Represents an existing document whose data is unknown (e.g. a document that was updated
     * without a known base document).
     */
    UNKNOWN_DOCUMENT;
  }

  /** Describes the `hasPendingWrites` state of a document. */
  private enum DocumentState {
    /** Local mutations applied via the mutation queue. Document is potentially inconsistent. */
    LOCAL_MUTATIONS,
    /** Mutations applied based on a write acknowledgment. Document is potentially inconsistent. */
    COMMITTED_MUTATIONS,
    /** No mutations applied. Document was sent to us by Watch. */
    SYNCED
  }

  private final DocumentKey key;
  private DocumentType documentType;
  private SnapshotVersion version;
  private ObjectValue value;
  private DocumentState documentState;

  public MutableDocument(DocumentKey key) {
    this(key, DocumentType.INVALID, SnapshotVersion.NONE, new ObjectValue(), DocumentState.SYNCED);
  }

  private MutableDocument(
      DocumentKey key,
      DocumentType documentType,
      SnapshotVersion version,
      ObjectValue value,
      DocumentState documentState) {
    this.key = key;
    this.version = version;
    this.documentType = documentType;
    this.documentState = documentState;
    this.value = value;
  }

  /**
   * Changes the document type to indicate that it exists and that its version and data are known.
   */
  public MutableDocument setFoundDocument(SnapshotVersion version, ObjectValue value) {
    this.version = version;
    this.documentType = DocumentType.FOUND_DOCUMENT;
    this.value = value;
    this.documentState = DocumentState.SYNCED;
    return this;
  }

  /** Changes the document type to indicate that it doesn't exist at the given version. */
  public MutableDocument setNoDocument(SnapshotVersion version) {
    this.version = version;
    this.documentType = DocumentType.NO_DOCUMENT;
    this.value = new ObjectValue();
    this.documentState = DocumentState.SYNCED;
    return this;
  }

  /**
   * Changes the document type to indicate that it exists at a given version but that is data is not
   * known (e.g. a document that was updated without a known base document).
   */
  public MutableDocument setUnknownDocument(SnapshotVersion version) {
    this.version = version;
    this.documentType = DocumentType.UNKNOWN_DOCUMENT;
    this.value = new ObjectValue();
    this.documentState = DocumentState.COMMITTED_MUTATIONS;
    return this;
  }

  public MutableDocument setCommittedMutations() {
    this.documentState = DocumentState.COMMITTED_MUTATIONS;
    return this;
  }

  public MutableDocument setLocalMutations() {
    this.documentState = DocumentState.LOCAL_MUTATIONS;
    return this;
  }

  @Override
  public DocumentKey getKey() {
    return key;
  }

  /**
   * Returns the version of this document if it exists or a version at which this document was
   * guaranteed to not exist.
   */
  public SnapshotVersion getVersion() {
    return version;
  }

  /** Returns whether local mutations were applied via the mutation queue. */
  @Override
  public boolean hasLocalMutations() {
    return documentState.equals(DocumentState.LOCAL_MUTATIONS);
  }

  /** Returns whether mutations were applied based on a write acknowledgment. */
  @Override
  public boolean hasCommittedMutations() {
    return documentState.equals(DocumentState.COMMITTED_MUTATIONS);
  }

  /**
   * Whether this document has a local mutation applied that has not yet been acknowledged by Watch.
   */
  @Override
  public boolean hasPendingWrites() {
    return hasLocalMutations() || hasCommittedMutations();
  }

  @Override
  public ObjectValue getData() {
    return value;
  }

  @Override
  public Value getField(FieldPath field) {
    return getData().get(field);
  }

  @Override
  public boolean isValidDocument() {
    return !documentType.equals(DocumentType.INVALID);
  }

  @Override
  public boolean isFoundDocument() {
    return documentType.equals(DocumentType.FOUND_DOCUMENT);
  }

  @Override
  public boolean isNoDocument() {
    return documentType.equals(DocumentType.NO_DOCUMENT);
  }

  @Override
  public boolean isUnknownDocument() {
    return documentType.equals(DocumentType.UNKNOWN_DOCUMENT);
  }

  @Override
  @NonNull
  public MutableDocument clone() {
    return new MutableDocument(key, documentType, version, value.clone(), documentState);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MutableDocument document = (MutableDocument) o;

    if (!key.equals(document.key)) return false;
    if (!version.equals(document.version)) return false;
    if (!documentType.equals(document.documentType)) return false;
    if (!documentState.equals(document.documentState)) return false;
    return value.equals(document.value);
  }

  @Override
  public int hashCode() {
    // We only use the key for the hashcode as all other document properties are mutable.
    // While mutable documents should not be uses as keys in collections, the hash code is used
    // in DocumentSet, which tracks Documents that are no longer being mutated but which are
    // backed by this class.
    return key.hashCode();
  }

  @Override
  public String toString() {
    return "Document{"
        + "key="
        + key
        + ", version="
        + version
        + ", type="
        + documentType
        + ", documentState="
        + documentState
        + ", value="
        + value
        + '}';
  }
}