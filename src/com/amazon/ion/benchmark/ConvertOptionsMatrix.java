package com.amazon.ion.benchmark;

import com.amazon.ion.IonStruct;

import java.util.List;
import java.util.Map;

/**
 * Represents all convert command options combinations, corresponding to all convert benchmark trials. A single
 * ConvertOptionsMatrix may yield multiple ConvertOptionsCombinations.
 */
public class ConvertOptionsMatrix extends OptionsMatrixBase {
    /**
     * @param optionsMatrix Map representing the options matrix for this command. The values of the map are either
     *                      scalar values or Lists of scalar values.
     */
    ConvertOptionsMatrix(Map<String, Object> optionsMatrix)  {
        super("convert", optionsMatrix);
    }

    @Override
    void parseCommandSpecificOptions(Map<String, Object> optionsMatrix, List<IonStruct> optionsCombinationStructs) {
        // no specific options for now
    }

}
