package io.antmedia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class AppSettingsJsonTest extends UnitTestBase<AppSettings> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void preservesIndependentLowLatencyDefaultsOnRoundTrip() throws Exception {
        AppSettings settings = mapper.readValue("{}", AppSettings.class);

        assertThat(settings.isLLHLSEnabled()).isFalse();
        assertThat(settings.isLLDashEnabled()).isTrue();

        AppSettings reloaded = mapper.readValue(mapper.writeValueAsString(settings), AppSettings.class);

        assertThat(reloaded.isLLHLSEnabled()).isFalse();
        assertThat(reloaded.isLLDashEnabled()).isTrue();
    }

    @Test
    void serializesLowLatencySettingsWithConsolePropertyNames() throws Exception {
        for (boolean enabled : new boolean[] {true, false}) {
            AppSettings settings = new AppSettings();
            settings.setLLHLSEnabled(enabled);
            settings.setLLDashEnabled(enabled);

            JsonNode json = mapper.readTree(mapper.writeValueAsString(settings));

            assertThat(json.has("lLHLSEnabled")).isTrue();
            assertThat(json.get("lLHLSEnabled").booleanValue()).isEqualTo(enabled);
            assertThat(json.has("lLDashEnabled")).isTrue();
            assertThat(json.get("lLDashEnabled").booleanValue()).isEqualTo(enabled);
            assertThat(json.has("llhlsenabled")).isFalse();
            assertThat(json.has("lldashEnabled")).isFalse();
        }
    }

    @Test
    void savesLowLatencyCheckboxChangesInFullSettingsPayload() throws Exception {
        for (boolean enabled : new boolean[] {true, false}) {
            AppSettings settings = new AppSettings();
            settings.setLLHLSEnabled(!enabled);
            settings.setLLDashEnabled(!enabled);
            ObjectNode json = mapper.valueToTree(settings);
            json.put("lLHLSEnabled", enabled);
            json.put("lLDashEnabled", enabled);

            AppSettings saved = mapper.readValue(mapper.writeValueAsString(json), AppSettings.class);

            assertThat(saved.isLLHLSEnabled()).isEqualTo(enabled);
            assertThat(saved.isLLDashEnabled()).isEqualTo(enabled);
            JsonNode reloaded = mapper.valueToTree(saved);
            assertThat(reloaded.get("lLHLSEnabled").booleanValue()).isEqualTo(enabled);
            assertThat(reloaded.get("lLDashEnabled").booleanValue()).isEqualTo(enabled);
        }
    }

    @Test
    void acceptsAlternateLowLatencyPropertyNames() throws Exception {
        for (boolean enabled : new boolean[] {true, false}) {
            String json = "{\"llhlsenabled\":" + enabled + ",\"lldashEnabled\":" + enabled + "}";

            AppSettings settings = mapper.readValue(json, AppSettings.class);

            assertThat(settings.isLLHLSEnabled()).isEqualTo(enabled);
            assertThat(settings.isLLDashEnabled()).isEqualTo(enabled);
        }
    }
}
