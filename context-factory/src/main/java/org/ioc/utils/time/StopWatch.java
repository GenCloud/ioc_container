/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.utils.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class StopWatch {
	private static final Logger log = LoggerFactory.getLogger(StopWatch.class);

	private static final BigDecimal MILLION = new BigDecimal("1000000");

	private long start;
	private long stop;

	private boolean isRunning;
	private boolean hasBeenUsedOnce;

	/**
	 * Start the stopwatch.
	 */
	public void start() {
		if (isRunning) {
			throw new IllegalStateException("Must stop before calling start again.");
		}
		//reset both start and stop
		start = System.nanoTime();
		stop = 0;
		isRunning = true;
		hasBeenUsedOnce = true;
	}

	/**
	 * Stop the stopwatch.
	 */
	public void stop() {
		if (!isRunning) {
			throw new IllegalStateException("Cannot stop if not currently running.");
		}

		stop = System.nanoTime();
		isRunning = false;
	}

	/**
	 * Express the "reading" on the stopwatch.
	 *
	 * <P>Example: <code>123.456 ms</code>. The resolution of timings on most systems
	 * is on the order of a few microseconds, so this style of presentation is usually
	 * appropriate for reflecting the real precision of most timers.
	 */
	@Override
	public String toString() {
		validateIsReadable();
		final StringBuilder result = new StringBuilder();
		BigDecimal value = new BigDecimal(get());
		value = value.divide(MILLION, 3, RoundingMode.HALF_EVEN);
		result.append(value);
		result.append(" ms");
		return result.toString();
	}

	/**
	 * Express the "reading" on the stopwatch as a numeric type, in nanoseconds.
	 */
	public long get() {
		validateIsReadable();
		return stop - start;
	}

	private void validateIsReadable() {
		if (isRunning) {
			throw new IllegalStateException("Cannot read a stopwatch which is still running.");
		}
		if (!hasBeenUsedOnce) {
			throw new IllegalStateException("Cannot read a stopwatch which has never been started.");
		}
	}

	public void log(String msg) {
		log.info(msg);
	}
}
