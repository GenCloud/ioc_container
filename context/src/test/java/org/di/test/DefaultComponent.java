package org.di.test;

import org.di.annotations.Component;
import org.di.annotations.LoadOpt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.di.annotations.LoadOpt.Opt.PROTOTYPE;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@Component
@LoadOpt(PROTOTYPE)
public class DefaultComponent {
    private static final Logger log = LoggerFactory.getLogger(DefaultComponent.class);
}
