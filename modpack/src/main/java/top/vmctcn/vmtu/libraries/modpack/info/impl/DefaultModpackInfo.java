package top.vmctcn.vmtu.libraries.modpack.info.impl;

import top.vmctcn.vmtu.libraries.modpack.info.api.ModpackInfo;

import java.util.Collections;

public class DefaultModpackInfo extends ModpackInfo {
    public DefaultModpackInfo() {
        this.formatVersion = 1;
        this.id = "example";
        this.name = "ExampleModpack 简体中文汉化";
        this.description = "Example patch pack information";
        this.language = "zh_cn";
        this.version = "1.0.0";
        this.modpackVersion = "0.1.0";
        this.modpackVersionRange = "[0.1.0]";
        this.authors = Collections.singletonList("ExampleAuthor");
        this.url = "https://vmct-cn.top/modpacks/example/";
        getModpack();
    }
}
