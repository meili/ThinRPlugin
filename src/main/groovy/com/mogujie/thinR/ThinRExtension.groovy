package com.mogujie.thinR;

public class ThinRExtension {

    public boolean skipThinR = false
    public boolean skipThinRDebug = true
    public int logLevel = 2
    public String buildVariants = null


    @Override
    public String toString() {
        String str =
                """
                skipThinR: ${skipThinR}
                skipThinRDebug: ${skipThinRDebug}
                logLevel: ${logLevel}
                buildVariants: ${buildVariants}
                """
        return str
    }
}
