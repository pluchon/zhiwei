package com.campus.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.campus.system.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusSystemApplicationTests {
  @Autowired private SysRoleMapper roles;

  @Test
  void migrationsAndSeedDataLoad() {
    assertThat(roles.selectCount(null)).isEqualTo(4);
  }
}
