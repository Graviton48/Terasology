package org.terasology.planetWorldGenerator.world.generator.facetProviders;

import org.terasology.entitySystem.Component;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2f;
import org.terasology.rendering.nui.properties.Range;
import org.terasology.utilities.procedural.BrownianNoise;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.PerlinNoise;
import org.terasology.utilities.procedural.SubSampledNoise;
import org.terasology.world.generation.ConfigurableFacetProvider;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Updates;
import org.terasology.world.generation.facets.SurfaceHeightFacet;

@Updates(@Facet(SurfaceHeightFacet.class))
public class MountainsProvider implements ConfigurableFacetProvider {

    private Noise mountainNoise;

    private MountainsConfiguration configuration = new MountainsConfiguration();

    @Override
    public void setSeed(long seed) {
        mountainNoise = new SubSampledNoise(new BrownianNoise(new PerlinNoise(seed + 2), 8), new Vector2f(0.001f, 0.001f), 1);
    }

    @Override
    public void process(GeneratingRegion region) {
        SurfaceHeightFacet facet = region.getRegionFacet(SurfaceHeightFacet.class);
        float mountainHeight = configuration.mountainHeight;
        // loop through every position on our 2d array
        Rect2i processRegion = facet.getWorldRegion();
        for (BaseVector2i position : processRegion.contents()) {
            // scale our max mountain height to noise (between -1 and 1)
            float additiveMountainHeight = mountainNoise.noise(position.getX(), position.getY()) * mountainHeight;
            // dont bother subtracting mountain height,  that will allow unaffected regions
            additiveMountainHeight = TeraMath.clamp(additiveMountainHeight, 0, mountainHeight);

            facet.setWorld(position, facet.getWorld(position) + additiveMountainHeight);
        }
    }

    @Override
    public String getConfigurationName()
    {
        return "Mountains";
    }

    @Override
    public Component getConfiguration()
    {
        return configuration;
    }

    @Override
    public void setConfiguration(Component configuration)
    {
        this.configuration = (MountainsConfiguration)configuration;
    }

    private static class MountainsConfiguration implements Component {
        @Range(min = 200, max = 500f, increment = 20f, precision = 2, description = "Mountain Height")
        private float mountainHeight = 400;
    }
}