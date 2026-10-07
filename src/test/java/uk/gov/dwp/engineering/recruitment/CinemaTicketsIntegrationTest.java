package uk.gov.dwp.engineering.recruitment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@SpringBootTest
@AutoConfigureMockMvc
class CinemaTicketsIntegrationTest {

  private static final String ADULT = "ADULT";
  private static final String CHILD = "CHILD";
  private static final String INFANT = "INFANT";

  @Autowired private ObjectMapper objectMapper;
  @Autowired private MockMvc mockMvc;
  @MockitoBean private PaymentService paymentService;
  @MockitoBean private SeatReservationService seatReservationService;

  @Test
  void givenValidBooking_whenCinemaBookings_thenReturnsBookingConfirmation() throws Exception {
    var adultTicketRequest = createTicketRequest(ADULT, 2);
    var childTicketRequest = createTicketRequest(CHILD, 2);
    var infantTicketRequest = createTicketRequest(INFANT, 1);
    var booking = createBooking(adultTicketRequest, childTicketRequest, infantTicketRequest);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accountId").value(1))
        .andExpect(jsonPath("$.seatCount").value(4))
        .andExpect(jsonPath("$.totalCost").value(86.98));

    verify(paymentService).debitAccount(1L, new BigDecimal("86.98"));
    verify(seatReservationService).reserveSeats(1L, 4L);
  }

  @Test
  void givenZeroAdultTicketCountWithChild_whenCinemaBookings_thenBadRequestResponseIsReturned()
      throws Exception {
    var childTicketRequest = createTicketRequest(CHILD, 1);
    var booking = createBooking(childTicketRequest);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("adult ticketCount must be greater than zero"))
        .andExpect(jsonPath("$.instance").value("/cinema/bookings"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void
      givenTotalTicketCountIsGreaterThanTwentyFive_whenCinemaBookings_thenBadRequestResponseIsReturned()
          throws Exception {
    var adultTicketRequest = createTicketRequest(ADULT, 26);
    var booking = createBooking(adultTicketRequest);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("total ticketCount must not exceed 25"))
        .andExpect(jsonPath("$.instance").value("/cinema/bookings"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void
      givenInfantTicketCountExceedsAdultTicketCount_whenCinemaBookings_thenBadRequestResponseIsReturned()
          throws Exception {
    var adultTicketRequest = createTicketRequest(ADULT, 1);
    var infantTicketRequest = createTicketRequest(INFANT, 2);
    var booking = createBooking(adultTicketRequest, infantTicketRequest);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            jsonPath("$.detail").value("infant ticketCount must not exceed adult ticketCount"))
        .andExpect(jsonPath("$.instance").value("/cinema/bookings"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenUnknownTicketRequestType_whenCinemaBookings_thenBadRequestResponseIsReturned()
      throws Exception {
    var seniorTicketRequest = createTicketRequest("SENIOR", 1);
    var booking = createBooking(seniorTicketRequest);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Failed to read request"))
        .andExpect(jsonPath("$.instance").value("/cinema/bookings"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void
      givenPaymentServiceThrowsException_whenCinemaBookings_thenInternalServerErrorResponseIsReturned()
          throws Exception {
    var adultTicketRequest = createTicketRequest(ADULT, 1);
    var booking = createBooking(adultTicketRequest);
    var testException = new RuntimeException("test exception");

    when(paymentService.debitAccount(anyLong(), any(BigDecimal.class))).thenThrow(testException);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Unable to confirm booking"))
        .andExpect(jsonPath("$.instance").value("/cinema/bookings"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.title").value("Internal Server Error"));

    verifyNoInteractions(seatReservationService);
  }

  @Test
  void
      givenSeatReservationServiceThrowsException_whenCinemaBookings_thenInternalServerErrorResponseIsReturned()
          throws Exception {
    var adultTicketRequest = createTicketRequest(ADULT, 1);
    var booking = createBooking(adultTicketRequest);
    var testException = new RuntimeException("test exception");

    when(seatReservationService.reserveSeats(anyLong(), anyLong())).thenThrow(testException);

    mockMvc
        .perform(
            post("/cinema/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Unable to confirm booking"))
        .andExpect(jsonPath("$.instance").value("/cinema/bookings"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.title").value("Internal Server Error"));

    verify(paymentService).debitAccount(1L, new BigDecimal("25.99"));
  }

  private ObjectNode createTicketRequest(String type, int ticketCount) {
    var ticketRequest = objectMapper.createObjectNode();
    ticketRequest.put("type", type);
    ticketRequest.put("ticketCount", ticketCount);
    return ticketRequest;
  }

  private ObjectNode createBooking(ObjectNode... ticketRequests) {
    var ticketRequestsNode = objectMapper.createArrayNode();
    for (var ticketRequest : ticketRequests) {
      ticketRequestsNode.add(ticketRequest);
    }

    var booking = objectMapper.createObjectNode();
    booking.put("accountId", 1);
    booking.set("ticketRequests", ticketRequestsNode);
    return booking;
  }
}
