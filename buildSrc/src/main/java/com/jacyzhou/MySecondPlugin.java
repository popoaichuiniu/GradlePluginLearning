package com.jacyzhou;

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MySecondPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        System.out.println("22222222222222222");
        BaseAppModuleExtension extend = (BaseAppModuleExtension) target.getExtensions().findByName("android");
        extend.registerTransform(new MyTransform());
    }
}
