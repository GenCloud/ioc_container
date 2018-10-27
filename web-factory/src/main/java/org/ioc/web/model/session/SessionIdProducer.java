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
package org.ioc.web.model.session;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SessionIdProducer {
	private ConcurrentLinkedQueue<SecureRandom> queue;
	private int length;

	SessionIdProducer() {
		queue = new ConcurrentLinkedQueue<>();
		length = 16;
	}

	public SessionIdProducer(int length) {
		this.length = length;
	}

	private void getRandomBytes(byte[] bytes) {
		SecureRandom random = queue.poll();
		if (random == null) {
			random = new SecureRandom();
		}
		random.nextBytes(bytes);
		queue.add(random);
	}

	public String generateSessionId() {
		byte random[] = new byte[16];

		StringBuilder buffer = new StringBuilder(2 * length);

		int resultLenBytes = 0;

		while (resultLenBytes < length) {
			getRandomBytes(random);
			for (int j = 0; j < random.length && resultLenBytes < length; j++) {
				byte b1 = (byte) ((random[j] & 0xf0) >> 4);
				byte b2 = (byte) (random[j] & 0x0f);
				if (b1 < 10) {
					buffer.append((char) ('0' + b1));
				} else {
					buffer.append((char) ('A' + (b1 - 10)));
				}
				if (b2 < 10) {
					buffer.append((char) ('0' + b2));
				} else {
					buffer.append((char) ('A' + (b2 - 10)));
				}
				resultLenBytes++;
			}
		}
		return buffer.toString();
	}
}
