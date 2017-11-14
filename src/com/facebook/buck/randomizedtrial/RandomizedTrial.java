/*
 * Copyright 2017-present Facebook, Inc.
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

package com.facebook.buck.randomizedtrial;

import com.facebook.buck.log.Logger;
import com.facebook.buck.model.BuildId;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.hash.Hashing;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Simple implementation of A/B testing. Each RandomizedTrial selects a group to which buck instance
 * belongs to.
 */
public class RandomizedTrial {
  private static final Supplier<String> HOSTNAME_SUPPLIER =
      Suppliers.memoize(
          () -> {
            try {
              return java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
              return "unable.to.determine.host";
            }
          });

  private static final Logger LOG = Logger.get(RandomizedTrial.class);

  private RandomizedTrial() {}

  /**
   * Returns a group for trial with given name.
   *
   * <p>This choice is stable for each test/user/hostname.
   *
   * @param name name of trial.
   * @param enumClass Class of an enum which conforms to {@link WithProbability} interface.
   */
  public static <T extends Enum<T> & WithProbability> T getGroupStable(
      String name, Class<T> enumClass) {
    return selectGroup(name, enumClass, getPoint(name));
  }

  /**
   * Returns a group for trial with given name.
   *
   * <p>This choice is stable for a particular buildId/test/user/hostname.
   *
   * @param name name of trial.
   * @param enumClass Class of an enum which conforms to {@link WithProbability} interface.
   */
  public static <T extends Enum<T> & WithProbability> T getGroup(
      String name, BuildId buildId, Class<T> enumClass) {
    return selectGroup(name, enumClass, getPoint(name, buildId.toString()));
  }

  private static <T extends Enum<T> & WithProbability> T selectGroup(
      String name, Class<T> enumClass, double point) {
    EnumSet<T> enumSet = EnumSet.allOf(enumClass);

    double sumOfAllProbabilities = enumSet.stream().mapToDouble(x -> x.getProbability()).sum();
    Preconditions.checkArgument(
        sumOfAllProbabilities == 1.0,
        "RandomizedTrial '%s' is misconfigured: sum of probabilities of all groups must be "
            + "equal 1.0, but it is %f",
        name,
        sumOfAllProbabilities);

    double remainder = point;
    for (T value : enumSet) {
      remainder -= value.getProbability();
      if (remainder < 0) {
        return value;
      }
    }

    throw new IllegalStateException(
        String.format("Test %s was unable to pick a value. point: %f", name, point));
  }

  @VisibleForTesting
  static double getPoint(String... names) {
    return getPointForKey(getKey(names));
  }

  /**
   * Return a double uniformly distributed between {@code [0, 1)} derived from the {@code key}.
   *
   * @param key Key which point we are looking for.
   * @return Value from 0.0 (inclusive) to 1.0 (exclusive).
   */
  private static double getPointForKey(String key) {
    return new Random(Hashing.sha384().hashString(key, StandardCharsets.UTF_8).asLong())
        .nextDouble();
  }

  private static String getKey(String... names) {
    String username = System.getProperty("user.name");
    if (username == null) {
      username = "unknown";
    }
    String hostname = HOSTNAME_SUPPLIER.get();
    String result = username + "@" + hostname + "/" + Joiner.on("/").join(names);
    LOG.debug("Determined key: '%s'", result);
    return result;
  }
}
