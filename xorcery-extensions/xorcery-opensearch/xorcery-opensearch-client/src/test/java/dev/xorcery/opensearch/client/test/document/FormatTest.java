package dev.xorcery.opensearch.client.test.document;

import dev.xorcery.util.Resources;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Optional;

@Disabled("random tests")
public class FormatTest {

    @Test
    public void formatTest()
    {
        System.out.println(String.format("numbers-%tF", System.currentTimeMillis()));
        System.out.println(String.format("numbers", System.currentTimeMillis()));
    }

    @Test
    public void resourceTest()
    {
        Optional<URL> resource = Resources.getResource("opensearch/templates/components/common.yaml");
        System.out.println(resource);
    }
}
