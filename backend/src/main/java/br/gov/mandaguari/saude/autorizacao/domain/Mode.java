package br.gov.mandaguari.saude.autorizacao.domain;

/**
 * The four GeneXus per-program access modes (pisauthorized). Each permission row carries one flag per
 * mode; {@code 1} grants, anything else denies.
 * <ul>
 *   <li>{@code CON} — consultar (read)</li>
 *   <li>{@code INC} — incluir (create)</li>
 *   <li>{@code ALT} — alterar (update)</li>
 *   <li>{@code EXC} — excluir (delete)</li>
 * </ul>
 */
public enum Mode { CON, INC, ALT, EXC }
