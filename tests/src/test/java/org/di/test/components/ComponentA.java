package org.di.test.components;

import org.di.context.annotations.IoCComponent;
import org.di.context.annotations.LoadOpt;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@IoCComponent
@LoadOpt(LoadOpt.Opt.PROTOTYPE)
public class ComponentA {
    @Override
    public String toString() {
        return "ComponentA{hash:" + Integer.toHexString(hashCode()) + "}";
    }
}
