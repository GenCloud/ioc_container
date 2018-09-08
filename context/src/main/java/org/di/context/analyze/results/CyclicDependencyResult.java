package org.di.context.analyze.results;

import org.di.context.analyze.enums.CyclicDependencyState;

import static org.di.context.analyze.enums.CyclicDependencyState.FALSE;

/**
 * @author GenCloud
 * @date 06.09.2018
 */
public class CyclicDependencyResult {
    private CyclicDependencyState cyclicDependencyState;
    private String throwMessage;

    public CyclicDependencyResult(CyclicDependencyState cyclicDependencyState) {
        this.cyclicDependencyState = cyclicDependencyState;
    }

    public CyclicDependencyResult(String throwMessage) {
        cyclicDependencyState = FALSE;
        this.throwMessage = throwMessage;
    }

    public CyclicDependencyState getCyclicDependencyState() {
        return cyclicDependencyState;
    }

    public String getThrowMessage() {
        return throwMessage;
    }
}
