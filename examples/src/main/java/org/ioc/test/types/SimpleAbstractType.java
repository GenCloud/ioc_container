package org.ioc.test.types;

import org.ioc.annotations.context.IoCComponent;
import org.ioc.annotations.context.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 11/2018
 */
@IoCComponent
public class SimpleAbstractType extends AbstractType {
	private static final Logger log = LoggerFactory.getLogger(SimpleAbstractType.class);

	@PostConstruct
	public void init() {
		log.info(super.toString());
	}
}
