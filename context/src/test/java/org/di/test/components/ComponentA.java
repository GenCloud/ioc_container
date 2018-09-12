package org.di.test.components;

import org.di.annotations.IoCComponent;
import org.di.annotations.LoadOpt;

import static org.di.annotations.LoadOpt.Opt.PROTOTYPE;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@IoCComponent
@LoadOpt(PROTOTYPE)
public class ComponentA {
    @Override
    public String toString() {
        return "ComponentA{hash:" + Integer.toHexString(hashCode()) + "}";
    }
}
