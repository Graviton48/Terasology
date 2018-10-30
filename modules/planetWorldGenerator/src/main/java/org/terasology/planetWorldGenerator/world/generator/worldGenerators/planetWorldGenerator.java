import org.terasology.engine.SimpleUri;
import org.terasology.registry.In;
import org.terasology.world.generation.BaseFacetedWorldGenerator;
import org.terasology.world.generation.WorldBuilder;
import org.terasology.world.generator.RegisterWorldGenerator;
import org.terasology.world.generator.plugin.WorldGeneratorPluginLibrary;
import org.terasology.planetWorldGenerator.world.generator.worldGenerators.;

@RegisterWorldGenerator(id = "planetWorld", displayName = "Planet World")
public class PlanetWorldGenerator extends BaseFacetedWorldGenerator {
    @In
    private WorldGeneratorPluginLibrary worldGeneratorPluginLibrary;

    public PlanetWorldGenerator(SimpleUri uri) {
        super(uri);
    }

    @Override
    protected WorldBuilder createWorld() {
        return new WorldBuilder(worldGeneratorPluginLibrary);
    }
}