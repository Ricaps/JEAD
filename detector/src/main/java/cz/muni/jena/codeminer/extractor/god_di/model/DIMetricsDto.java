package cz.muni.jena.codeminer.extractor.god_di.model;

import cz.muni.jena.inference.dto.BaseDto;

/**
 * DTO compatible with inference server
 * @param loc line of codes
 * @param cc cyclomatic complexity
 * @param nooa number of object attributes (= number if injected fields)
 * @param LCOM5 Lack of Coherence of Methods 5
 * @param noom number of object methods
 * @param nocm number of class (static) methods
 */
public record DIMetricsDto(
        Integer loc,
        Long cc,
        Long nooa,
        Double LCOM5,
        Integer noom,
        Integer nocm
) implements BaseDto {

}
