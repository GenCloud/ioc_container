package org.ioc.web.handler.metadata;

import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public enum TransferEncoding {
	NON_CHUNKED("non-chunked"),
	CHUNKED("chunked"),
	EVENT_STREAM("event-stream");

	private final String option;

	TransferEncoding(String option) {
		this.option = option;
	}

	public static TransferEncoding ofOption(String option) {
		if (Objects.equals(option, NON_CHUNKED.option)) {
			return NON_CHUNKED;
		}

		if (Objects.equals(option, CHUNKED.option)) {
			return CHUNKED;
		}

		if (Objects.equals(option, EVENT_STREAM.option)) {
			return EVENT_STREAM;
		}

		throw new IllegalArgumentException("Unexpected option: " + option);
	}
}
