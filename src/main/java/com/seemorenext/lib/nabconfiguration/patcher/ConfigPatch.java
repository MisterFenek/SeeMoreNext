package com.seemorenext.lib.nabconfiguration.patcher;

import com.seemorenext.lib.nabconfiguration.patcher.jobs.PatchJob;
import com.seemorenext.lib.nabconfiguration.patcher.structure.YamlFile;
import com.seemorenext.lib.nabconfiguration.patcher.structure.parser.YamlFileParser;

import java.io.*;
import java.util.List;

public class ConfigPatch {
    private List<PatchJob> patchJobs;

    ConfigPatch(List<PatchJob> patchJobs) {
        this.patchJobs = patchJobs;
    }

    public void patchFile(File file) throws IOException {
        YamlFile yamlFile = YamlFileParser.parse(file);
        for (PatchJob patchJob : patchJobs) {
            patchJob.modify(yamlFile);
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            for (String line : yamlFile.toLines()) {
                writer.println(line);
            }
        }
    }

}
