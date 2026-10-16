package com.university.booking.client;

import com.university.booking.dto.BookingDto;
import com.university.booking.dto.BookingPageDto;
import com.university.booking.enums.BookingStatus;
import com.university.booking.dto.CartDto;
import com.university.booking.dto.BookingRequestDto;
import com.university.booking.dto.CategoryDto;
import com.university.booking.dto.CommentRequestDto;
import com.university.booking.dto.ErrorResponseDto;
import com.university.booking.dto.OrganizationDto;
import com.university.booking.dto.OrganizationStatsReportDto;
import com.university.booking.dto.PersonCreateRequest;
import com.university.booking.dto.PersonDto;
import com.university.booking.dto.ReasonRequestDto;
import com.university.booking.dto.RoomDto;
import com.university.booking.dto.RoomScheduleDto;
import com.university.booking.exception.AccessDeniedException;
import com.university.booking.exception.BackendException;
import com.university.booking.exception.BotException;
import com.university.booking.exception.FeatureUnavailableException;
import com.university.booking.exception.NotRegisteredException;
import com.university.booking.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendClient {

    private final RestTemplate restTemplate;

    @Value("${backend.url}")
    private String baseUrl;

    public List<RoomDto> getRooms() {
        return call("GET /api/rooms", null,
                () -> asList(restTemplate.getForObject(baseUrl + "/api/rooms", RoomDto[].class)));
    }

    public RoomDto getRoom(String id) {
        return call("GET /api/rooms/" + id, id,
                () -> requireBody(restTemplate.getForObject(baseUrl + "/api/rooms/" + id, RoomDto.class)));
    }

    public RoomScheduleDto getRoomSchedule(String id, LocalDate date) {
        return call("GET /api/rooms/" + id + "/schedule", id, () -> requireBody(restTemplate.getForObject(
                baseUrl + "/api/rooms/" + id + "/schedule?date=" + date, RoomScheduleDto.class)));
    }

    public BookingPageDto searchBookings(String personId, BookingStatus status, int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl + "/api/bookings/search?sort=createdAt&direction=desc")
                .append("&page=").append(page)
                .append("&size=").append(size);
        if (personId != null) url.append("&personId=").append(personId);
        if (status != null) url.append("&status=").append(status.name());
        return call("GET /api/bookings/search", null,
                () -> requireBody(restTemplate.getForObject(url.toString(), BookingPageDto.class)));
    }

    public List<BookingDto> getBookings() {
        return call("GET /api/bookings", null,
                () -> asList(restTemplate.getForObject(baseUrl + "/api/bookings", BookingDto[].class)));
    }

    public BookingDto getBooking(String id) {
        return call("GET /api/bookings/" + id, id,
                () -> requireBody(restTemplate.getForObject(baseUrl + "/api/bookings/" + id, BookingDto.class)));
    }

    public BookingDto createBooking(BookingRequestDto request) {
        return call("POST /api/bookings", null,
                () -> requireBody(restTemplate.postForObject(baseUrl + "/api/bookings", request, BookingDto.class)));
    }

    public BookingDto submitBooking(String id) {
        return call("PUT /api/bookings/" + id + "/submit", id, () -> requireBody(restTemplate.exchange(
                baseUrl + "/api/bookings/" + id + "/submit", HttpMethod.PUT, null, BookingDto.class).getBody()));
    }

    public BookingDto approveBooking(String id, String reason) {
        return call("PUT /api/bookings/" + id + "/approve", id, () -> requireBody(restTemplate.exchange(
                baseUrl + "/api/bookings/" + id + "/approve", HttpMethod.PUT, reasonBody(reason), BookingDto.class).getBody()));
    }

    public BookingDto rejectBooking(String id, String reason) {
        return call("PUT /api/bookings/" + id + "/reject", id, () -> requireBody(restTemplate.exchange(
                baseUrl + "/api/bookings/" + id + "/reject", HttpMethod.PUT, reasonBody(reason), BookingDto.class).getBody()));
    }

    public BookingDto addComment(String id, String text) {
        return call("POST /api/bookings/" + id + "/comments", id, () -> requireBody(restTemplate.postForObject(
                baseUrl + "/api/bookings/" + id + "/comments", new CommentRequestDto(text), BookingDto.class)));
    }

    public List<OrganizationDto> getOrganizations() {
        return call("GET /api/organizations", null,
                () -> asList(restTemplate.getForObject(baseUrl + "/api/organizations", OrganizationDto[].class)));
    }

    public OrganizationDto getOrganization(String id) {
        return call("GET /api/organizations/" + id, id,
                () -> requireBody(restTemplate.getForObject(baseUrl + "/api/organizations/" + id, OrganizationDto.class)));
    }

    public OrganizationStatsReportDto getOrganizationStats(LocalDate from, LocalDate to) {
        StringBuilder url = new StringBuilder(baseUrl + "/api/analytics/organizations");
        if (from != null || to != null) {
            url.append("?");
            if (from != null) url.append("from=").append(from).append("&");
            if (to != null) url.append("to=").append(to);
        }
        return call("GET /api/analytics/organizations", null,
                () -> requireBody(restTemplate.getForObject(url.toString(), OrganizationStatsReportDto.class)));
    }

    public void deleteBooking(String id) {
        call("DELETE /api/bookings/" + id, id, () -> {
            restTemplate.delete(baseUrl + "/api/bookings/" + id);
            return null;
        });
    }

    public List<PersonDto> getPersons() {
        return call("GET /api/persons", null,
                () -> asList(restTemplate.getForObject(baseUrl + "/api/persons", PersonDto[].class)));
    }

    public PersonDto getPerson(String id) {
        return call("GET /api/persons/" + id, id,
                () -> requireBody(restTemplate.getForObject(baseUrl + "/api/persons/" + id, PersonDto.class)));
    }

    public PersonDto createPerson(PersonCreateRequest request) {
        return call("POST /api/persons", null,
                () -> requireBody(restTemplate.postForObject(baseUrl + "/api/persons", request, PersonDto.class)));
    }

    public List<CategoryDto> getCategories() {
        return call("GET /api/categories", null,
                () -> asList(restTemplate.getForObject(baseUrl + "/api/categories", CategoryDto[].class)));
    }

    public CartDto getCart() {
        return call("GET /api/cart", null,
                () -> requireBody(restTemplate.getForObject(baseUrl + "/api/cart", CartDto.class)));
    }

    public CartDto addToCart(BookingRequestDto request) {
        return call("POST /api/cart/items", null,
                () -> requireBody(restTemplate.postForObject(baseUrl + "/api/cart/items", request, CartDto.class)));
    }

    public void clearCart() {
        call("DELETE /api/cart", null, () -> {
            restTemplate.delete(baseUrl + "/api/cart");
            return null;
        });
    }

    public List<BookingDto> checkoutCart() {
        return call("POST /api/cart/checkout", null,
                () -> asList(restTemplate.postForObject(baseUrl + "/api/cart/checkout", null, BookingDto[].class)));
    }

    private <T> T call(String endpoint, String resourceId, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new NotRegisteredException();
        } catch (HttpClientErrorException.Forbidden e) {
            throw new AccessDeniedException(errorMessage(e));
        } catch (HttpClientErrorException.NotFound e) {
            if (resourceId != null) throw new ResourceNotFoundException(resourceId);
            throw new BotException(errorMessage(e));
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.atWarn().addKeyValue("endpoint", endpoint).log("rate limit exceeded");
            throw new BotException("Слишком много запросов. Подождите минуту и попробуйте снова");
        } catch (HttpClientErrorException e) {
            throw new BotException(errorMessage(e));
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            log.atWarn().addKeyValue("endpoint", endpoint).log("backend feature unavailable");
            throw new FeatureUnavailableException(errorMessage(e));
        } catch (RestClientException e) {
            log.atError().addKeyValue("endpoint", endpoint).setCause(e).log("backend request failed");
            throw new BackendException();
        }
    }

    private static HttpEntity<ReasonRequestDto> reasonBody(String reason) {
        return reason == null || reason.isBlank() ? null : new HttpEntity<>(new ReasonRequestDto(reason));
    }

    private String errorMessage(HttpStatusCodeException e) {
        try {
            ErrorResponseDto body = e.getResponseBodyAs(ErrorResponseDto.class);
            if (body != null && body.getErrors() != null && !body.getErrors().isEmpty()) {
                return body.getMessage() + ":\n" + String.join("\n", body.getErrors());
            }
            if (body != null && body.getMessage() != null) {
                return body.getMessage();
            }
        } catch (RuntimeException ignored) {

        }
        return "Запрос отклонён сервером (" + e.getStatusCode().value() + ").";
    }

    private static <T> T requireBody(T body) {
        if (body == null) throw new BackendException();
        return body;
    }

    private static <T> List<T> asList(T[] array) {
        return array != null ? Arrays.asList(array) : List.of();
    }
}