/*
 * Copyright 2015-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.buck.rules.coercer;

import static com.facebook.buck.core.cell.TestCellBuilder.createCellRoots;
import static org.junit.Assert.assertEquals;

import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.io.filesystem.impl.FakeProjectFilesystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.junit.Before;
import org.junit.Test;

public class LogLevelTypeCoercerTest {

  private LogLevelTypeCoercer coercer;
  private ProjectFilesystem filesystem = new FakeProjectFilesystem();
  private Path pathFromRoot = Paths.get("third-party/java");

  @Before
  public void setUp() {
    coercer = new LogLevelTypeCoercer();
  }

  @Test
  public void coercesValidLogLevel() throws CoerceFailedException {
    Level expected = Level.WARNING;
    Level actual = coercer.coerce(createCellRoots(filesystem), filesystem, pathFromRoot, "WARNING");

    assertEquals(expected, actual);
  }

  @Test(expected = CoerceFailedException.class)
  public void throwsWhenCoercingUnknownLogLevel() throws CoerceFailedException {
    coercer.coerce(createCellRoots(filesystem), filesystem, pathFromRoot, "not a log level");
  }
}
