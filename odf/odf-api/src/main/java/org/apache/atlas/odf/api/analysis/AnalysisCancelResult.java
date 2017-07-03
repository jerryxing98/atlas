/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.odf.api.analysis;

public class AnalysisCancelResult {

	public enum State {
		NOT_FOUND,
		INVALID_STATE,
		SUCCESS
	}

	private State state;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

}