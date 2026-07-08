package com.lnatit.womm.data;

import com.lnatit.womm.WOMM;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber
public class TemplateManager extends SimpleJsonResourceReloadListener<Headless>
{
    public static final String NAME = "world_templates";

    @SubscribeEvent
    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(WOMM.id(NAME), INSTANCE);
    }

    public static final TemplateManager INSTANCE = new TemplateManager();
    private final Map<String, Template> templates = new HashMap<>();

    private TemplateManager() {
        super(Headless.CODEC, FileToIdConverter.json(NAME));
    }

    @Override
    protected void apply(Map<Identifier, Headless> preparations, ResourceManager manager, ProfilerFiller profiler) {
        profiler.push("womm_world_templates");
        this.templates.clear();

        for (var entry : preparations.entrySet()) {
            Headless template = entry.getValue();
            Optional<String> explicitIdentity = template.identity();
            String identity = entry.getKey().getPath();

            if (explicitIdentity.isPresent()) {
                identity = explicitIdentity.get();
                if (this.templates.containsKey(identity)) {
                    WOMM.LOGGER.error("Duplicate world template identity '{}' found, ignored!", identity);
                    continue;
                }
            }
            else {
                if (this.templates.containsKey(identity)) {
                    String fallbackIdentity = entry.getKey().toString();
                    if (this.templates.containsKey(fallbackIdentity)) {
                        WOMM.LOGGER.error("Duplicate world template identity '{}' and fallback '{}' found, ignored!",
                                          identity,
                                          fallbackIdentity);
                        continue;
                    }
                    identity = fallbackIdentity;
                }
            }
            this.templates.put(identity, template.withId(entry.getKey().withPath(identity), false));
        }

        profiler.pop();
    }

    public boolean isEmpty() {
        return this.templates.isEmpty();
    }

    public Set<String> getIdentities() {
        return this.templates.keySet();
    }

    public Optional<Template> getTemplate(String identity) {
        return Optional.ofNullable(this.templates.get(identity));
    }
}
