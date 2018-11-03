package org.terasology.planetWorldGenerator.world.generator.worldGenerators;
import org.terasology.core.world.generator.facetProviders.SeaLevelProvider;
import org.terasology.engine.SimpleUri;
import org.terasology.planetWorldGenerator.world.generator.facetProviders.MountainsProvider;
import org.terasology.planetWorldGenerator.world.generator.facetProviders.PlanetProvider;
import org.terasology.planetWorldGenerator.world.generator.rasterizers.PlanetRasterizer;
import org.terasology.registry.In;
import org.terasology.world.generation.BaseFacetedWorldGenerator;
import org.terasology.world.generation.WorldBuilder;
import org.terasology.world.generator.RegisterWorldGenerator;
import org.terasology.world.generator.plugin.WorldGeneratorPluginLibrary;


@RegisterWorldGenerator(id = "planetWorld", displayName = "Planet World")
public class planetWorldGenerator extends BaseFacetedWorldGenerator {
    @In
    private WorldGeneratorPluginLibrary worldGeneratorPluginLibrary;

    public planetWorldGenerator(SimpleUri uri) {
        super(uri);
    }

    @Override
    protected WorldBuilder createWorld() {
        return new WorldBuilder(worldGeneratorPluginLibrary)
                .addProvider(new PlanetProvider())
                .addProvider(new SeaLevelProvider(0))
                .addRasterizer(new PlanetRasterizer())
                .addProvider(new MountainsProvider())
                .addPlugins();
    }
}