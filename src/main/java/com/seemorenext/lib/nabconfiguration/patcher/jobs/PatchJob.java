package com.seemorenext.lib.nabconfiguration.patcher.jobs;

import com.seemorenext.lib.nabconfiguration.patcher.structure.YamlFile;

public interface PatchJob {

    void modify(YamlFile yamlFile);

}
