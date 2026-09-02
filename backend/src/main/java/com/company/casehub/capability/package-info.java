/**
 * Capability Library module (Phase 5) — Database Schema V1.0 §10.
 *
 * <p>Owns the global Capability Tree: {@code casehub.capabilities}, a self-referencing
 * table of arbitrary depth. It answers <em>"what capabilities does a device have"</em>.
 *
 * <p>Explicitly out of scope — these belong to later phases and must NOT leak into
 * this package:
 * <ul>
 *   <li>Project Capability (the {@code YES / NO / UNKNOWN} conclusion per project)</li>
 *   <li>Capability Update Request / review workflow</li>
 *   <li>Generation Rule and the generation engine</li>
 * </ul>
 *
 * <p>The Category tree is a separate tree and must not be merged with this one.
 */
package com.company.casehub.capability;
