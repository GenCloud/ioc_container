/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of DI (IoC) Container Project.
 *
 * DI (IoC) Container Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DI (IoC) Container Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DI (IoC) Container Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.utils;

import java.io.PrintStream;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class BannerUtils {
	private static final String[] BANNER = {"",
			" _____       _____  _____ _             _",
			"|_   _|     / ____|/ ____| |           | |",
			"  | |  ___ | |    | (___ | |_ __ _ _ __| |_ ___ _ __",
			"  | | / _ \\| |     \\___ \\| __/ _` | '__| __/ _ \\ '__|",
			" _| || (_) | |____ ____) | || (_| | |  | ||  __/ |",
			"|_____\\___/ \\_____|_____/ \\__\\__,_|_|   \\__\\___|_|"};

	private static final String LAST = "    IoC Starter        (ver.%version%)\r\n";

	public static void printBanner(PrintStream printStream) {
		for (String s : BANNER) {
			printStream.println(s);
		}
		final String version = BannerUtils.class.getPackage().getImplementationVersion();
		String last = LAST.replace("%version%", version == null ? "unknown" : version);
		printStream.println(last);
	}
}
