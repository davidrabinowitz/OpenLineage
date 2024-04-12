/*
/* Copyright 2018-2024 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark.agent;

import io.openlineage.client.transports.TokenProvider;

public class FakeTokenProvider implements TokenProvider {

  static final String FIXED_TOKEN = "from_fake";

  @Override
  public String getToken() {
    return FIXED_TOKEN;
  }
}
