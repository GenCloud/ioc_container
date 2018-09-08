package org.di.context.analyze.results;

import org.di.context.analyze.enums.ClassStateInjection;

import static org.di.context.analyze.enums.ClassStateInjection.GRAMMAR_THROW_EXCEPTION;

/**
 * @author GenCloud
 * @date 05.09.2018
 */
public class ClassAnalyzeResult {
    private ClassStateInjection classStateInjection;
    private String throwableMessage;

    public ClassAnalyzeResult(ClassStateInjection classStateInjection) {
        this.classStateInjection = classStateInjection;
    }

    public ClassAnalyzeResult(String throwableMessage) {
        this.throwableMessage = throwableMessage;
        classStateInjection = GRAMMAR_THROW_EXCEPTION;
    }

    public ClassStateInjection getClassStateInjection() {
        return classStateInjection;
    }

    public String getThrowableMessage() {
        return throwableMessage;
    }
}
