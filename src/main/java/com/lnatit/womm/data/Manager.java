package com.lnatit.womm.data;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class Manager extends SimpleJsonResourceReloadListener<Headless>
{
    public static final Manager INSTANCE = new Manager();
    public final Map<String, Templatel> templates = new HashMap<>();

    private Manager() {
        super(Headless.CODEC, FileToIdConverter.json("world_templates"));
    }

    @Override
    protected void apply(Map<Identifier, Headless> preparations, ResourceManager manager, ProfilerFiller profiler) {
        profiler.push("womm_world_templates");
        this.templates.clear();

        for (var entry : preparations.entrySet()) {
            var template = entry.getValue();
//            if (this.templates.containsKey(template.identity())) {
//                WOMM.LOGGER.error("Duplicate world template identity '{}' found, ignored!", template.identity());
//                continue;
//            }
//            this.templates.put(template.identity(), template);
        }

        profiler.pop();
    }
}
