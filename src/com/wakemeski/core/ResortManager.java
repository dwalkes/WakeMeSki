/*
 * Copyright (C) 2010 Dan Walkes, Andy Doan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wakemeski.core;

/**
 * Describe a resore management interface.
 * Decouples persistent storage from classes which work on a list of resorts
 */
public interface ResortManager {
	/**
	 * @return the list of resorts currently selected by the user for monitoring
	 */
	public abstract Resort[] getResorts();
}
