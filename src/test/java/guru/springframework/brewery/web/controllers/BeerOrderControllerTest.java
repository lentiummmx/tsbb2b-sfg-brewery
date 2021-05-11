package guru.springframework.brewery.web.controllers;

import guru.springframework.brewery.services.BeerOrderService;
import guru.springframework.brewery.web.model.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerOrderController.class)
class BeerOrderControllerTest {

    @MockBean
    BeerOrderService beerOrderService;

    @Autowired
    MockMvc mockMvc;

    BeerOrderDto beerOrderDto;

    @BeforeEach
    void setUp() {
        BeerDto validBeer = BeerDto.builder().id(UUID.randomUUID())
                .version(1)
                .beerName("Beer1")
                .beerStyle(BeerStyleEnum.PALE_ALE)
                .price(new BigDecimal("12.99"))
                .quantityOnHand(4)
                .upc(12345678912L)
                .createdDate(OffsetDateTime.now())
                .lastModifiedDate(OffsetDateTime.now())
                .build();

        beerOrderDto = BeerOrderDto.builder()
                .id(UUID.randomUUID())
                .version(1)
                .createdDate(OffsetDateTime.now())
                .lastModifiedDate(OffsetDateTime.now())
                .customerId(UUID.randomUUID())
                .beerOrderLines(List.of(BeerOrderLineDto.builder().beerId(validBeer.getId()).build()))
                .orderStatus(OrderStatusEnum.NEW)
                .orderStatusCallbackUrl("some-url")
                .customerRef("customerRef")
                .build();
    }

    @AfterEach
    void tearDown() {
        reset(beerOrderService);
    }

    @Test
    void getOrder() throws Exception {
        given(beerOrderService.getOrderById(any(), any())).willReturn(beerOrderDto);

        mockMvc.perform(get("/api/v1/customers/{customerId}/orders/{orderId}", beerOrderDto.getCustomerId(), beerOrderDto.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(beerOrderDto.getId().toString())))
                .andExpect(jsonPath("$.customerId", is(beerOrderDto.getCustomerId().toString())));
    }

    @DisplayName("Test list orders ops - ")
    @Nested
    public class TestListOrdersOps {

        @Captor
        ArgumentCaptor<UUID> customerArgCaptor;

        @Captor
        ArgumentCaptor<PageRequest> pageRequestArgCaptor;

        BeerOrderPagedList beerOrderPagedList;

        @BeforeEach
        void setUp() {
            List<BeerOrderDto> beerOrders = new ArrayList<>();
            beerOrders.add(beerOrderDto);
            beerOrders.add(BeerOrderDto.builder()
                    .id(UUID.randomUUID())
                    .version(1)
                    .createdDate(OffsetDateTime.now())
                    .lastModifiedDate(OffsetDateTime.now())
                    .customerId(beerOrderDto.getCustomerId())
                    .beerOrderLines(null)
                    .orderStatus(OrderStatusEnum.NEW)
                    .orderStatusCallbackUrl("some-url-v2")
                    .customerRef("customerRef2")
                    .build());

            beerOrderPagedList = new BeerOrderPagedList(beerOrders, PageRequest.of(1, 1), 2);

            given(beerOrderService.listOrders(
                    customerArgCaptor.capture(),
                    pageRequestArgCaptor.capture()
            )).willReturn(beerOrderPagedList);
        }

        @DisplayName("Test list beer orders - no params")
        @Test
        void listOrders() throws Exception {
            mockMvc.perform(get("/api/v1/customers/{customerId}/orders", beerOrderDto.getCustomerId()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(beerOrderDto.getId().toString())));
        }
    }
}