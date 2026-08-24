package com.aiops.converter;

import com.aiops.vo.DistributionItemVO;
import com.aiops.vo.KeywordItemVO;
import com.aiops.vo.TrendItemVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisJsonConverterTest {

    private final AnalysisJsonConverter converter = new AnalysisJsonConverter(new ObjectMapper());

    @Test
    void parseKeywordsReturnsKeywordItems() {
        List<KeywordItemVO> result = converter.parseKeywords("[{\"keyword\":\"entrega\",\"count\":3}]");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyword()).isEqualTo("entrega");
        assertThat(result.get(0).getCount()).isEqualTo(3);
    }

    @Test
    void parseDistributionsReturnsDistributionItems() {
        List<DistributionItemVO> result = converter.parseDistributions("[{\"name\":\"logistics\",\"count\":2}]");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("logistics");
        assertThat(result.get(0).getCount()).isEqualTo(2);
    }

    @Test
    void parseTrendsReturnsTrendItems() {
        List<TrendItemVO> result = converter.parseTrends("[{\"timeBucket\":\"2018-05\",\"commentCount\":4,\"negativeCount\":1,\"negativeRate\":0.25,\"avgScore\":4.5}]");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTimeBucket()).isEqualTo("2018-05");
        assertThat(result.get(0).getCommentCount()).isEqualTo(4);
        assertThat(result.get(0).getNegativeRate()).isEqualByComparingTo("0.25");
    }

    @Test
    void stringListRoundTripKeepsBusinessTags() {
        String json = converter.toJsonArray(List.of("delivery_delay", "vip"));

        assertThat(converter.parseStringList(json)).containsExactly("delivery_delay", "vip");
    }
}
