package com.amazon.ion.benchmark;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a combination of convert command options that corresponds to a single convert benchmark trial.
 */
class ConvertOptionsCombination extends OptionsCombinationBase {

    /**
     * @param serializedOptionsCombination text Ion representation of the options combination.
     * @throws IOException if thrown while parsing the options combination.
     */
    ConvertOptionsCombination(String serializedOptionsCombination) throws IOException {
        super(serializedOptionsCombination);
    }

    @Override
    protected MeasurableTask createMeasurableTask(Path inputFile) throws Exception {
        return null;
    }

}
