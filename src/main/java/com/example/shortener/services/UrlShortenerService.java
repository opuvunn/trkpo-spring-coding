package com.example.shortener.services;

import com.example.shortener.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlShortenerService {

    private static final long DEFAULT_TTL = 10L;
    private static final TimeUnit DEFAULT_TTL_UNIT = TimeUnit.HOURS;

    private static final long MAX_TTL_MILLIS =
            TimeUnit.HOURS.toMillis(48);

    private static final int MAX_VIP_KEY_LENGTH = 255;

    @Autowired
    private RedirectionRepo repo;

    @Autowired
    private RandomKeyGen gen;

    @Value("${shortKeySize}")
    private Integer shortKeySize = 3;

    @Value("${adminKeySize}")
    private Integer adminKeySize = 10;

    @Value("${application.domain}")
    private String appDomain = "localhost";

    @Value("${application.protocol}")
    private String protocol = "http";

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    UrlValidator validator;

    // Старое API сохраняем без изменений:

    public Pair<String, String> shorten(String longUrl) {
        return shorten(longUrl, null, null, null);
    }

    public Pair<String, String> shorten(
            String longUrl,
            String vipKey,
            Long timeToLive,
            TimeUnit timeToLiveUnit) {

        String validationError =
                validator.validateAndGetError(longUrl);

        if (validationError != null) {
            throw new InvalidUrlException(validationError);
        }

        /*
         * Если пользователь вообще не использует новые параметры,
         * сохраняем старое поведение без TTL.
         *
         * Если используется хотя бы один новый параметр,
         * отсутствующие части TTL берём из OpenAPI defaults.
         */
        boolean newOptionsUsed =
                vipKey != null
                        || timeToLive != null
                        || timeToLiveUnit != null;

        if (newOptionsUsed) {
            if (timeToLive == null) {
                timeToLive = DEFAULT_TTL;
            }

            if (timeToLiveUnit == null) {
                timeToLiveUnit = DEFAULT_TTL_UNIT;
            }
        }

        validateTimeToLive(timeToLive, timeToLiveUnit);

        String shortKey;

        if (vipKey == null) {
            shortKey = gen.generateKey(shortKeySize);
        } else {
            validateVipKey(vipKey);
            ensureVipKeyAvailable(vipKey);
            shortKey = vipKey;
        }

        String secretKey = gen.generateKey(adminKeySize);

        Date expirationDate =
                calculateExpirationDate(
                        timeToLive,
                        timeToLiveUnit
                );

        Redirection redirection =
                new Redirection(
                        longUrl,
                        shortKey,
                        secretKey,
                        expirationDate
                );

        try {
            repo.save(redirection);
        } catch (DataIntegrityViolationException dive) {
            throw new AlreadyMappedShortKeyException(
                    shortKey,
                    dive
            );
        }

        return Pair.of(
                formatShortUrl(shortKey),
                secretKey
        );
    }

    private void validateVipKey(String vipKey) {

        if (vipKey.isBlank()) {
            throw new InvalidRequestException(
                    "vipKey must not be empty"
            );
        }

        if (vipKey.length() > MAX_VIP_KEY_LENGTH) {
            throw new InvalidRequestException(
                    "vipKey is too long"
            );
        }

        if (!vipKey.matches("[a-zA-Z0-9_-]+")) {
            throw new InvalidRequestException(
                    "vipKey contains invalid characters"
            );
        }
    }

    private void ensureVipKeyAvailable(String vipKey) {

        Optional<Redirection> existing =
                repo.findByShortKey(vipKey);

        if (existing.isEmpty()) {
            return;
        }

        Redirection redirection = existing.get();

        /*
         * Истёкшая VIP-ссылка больше не должна занимать
         * красивый короткий ключ.
         */
        if (redirection.isExpired()) {
            repo.delete(redirection);
            return;
        }

        throw new AlreadyMappedShortKeyException(
                vipKey,
                null
        );
    }

    private void validateTimeToLive(
            Long timeToLive,
            TimeUnit timeToLiveUnit) {

        /*
         * Оба null здесь означают только старый формат запроса,
         * в котором TTL вообще отсутствует.
         */
        if (timeToLive == null && timeToLiveUnit == null) {
            return;
        }

        if (timeToLive == null || timeToLiveUnit == null) {
            throw new InvalidRequestException(
                    "Invalid TTL parameters"
            );
        }

        if (timeToLive < 0) {
            throw new InvalidRequestException(
                    "timeToLive must not be negative"
            );
        }

        long ttlMillis =
                timeToLiveUnit.toMillis(timeToLive);

        if (ttlMillis > MAX_TTL_MILLIS) {
            throw new InvalidRequestException(
                    "timeToLive must not exceed 48 hours"
            );
        }
    }

    private Date calculateExpirationDate(
            Long timeToLive,
            TimeUnit timeToLiveUnit) {

        if (timeToLive == null) {
            return null;
        }

        long expirationMillis =
                System.currentTimeMillis()
                        + timeToLiveUnit.toMillis(timeToLive);

        return new Date(expirationMillis);
    }

    private String formatShortUrl(String tail) {
        return protocol
                + "://"
                + appDomain
                + ":"
                + serverPort
                + "/"
                + tail;
    }

    public Redirection resolve(String shortKey)
            throws RedirectionNotFoundException {

        Optional<Redirection> redirectionO =
                repo.findByShortKey(shortKey);

        if (redirectionO.isPresent()) {

            Redirection redirection =
                    redirectionO.get();

            if (redirection.isExpired()) {
                /*
                 * Истёкшая ссылка считается отсутствующей,
                 * а ключ освобождаем.
                 */
                repo.delete(redirection);

                throw new RedirectionNotFoundException(
                        shortKey
                );
            }

            redirection.incrementUsageCount();
            repo.save(redirection);

            return redirection;
        }

        throw new RedirectionNotFoundException(shortKey);
    }

    public Redirection findBySecretKey(String secretKey) {

        Optional<Redirection> redirectionO =
                repo.findBySecretKey(secretKey);

        if (redirectionO.isPresent()) {
            return redirectionO.get();
        }

        throw new RedirectionNotFoundException(secretKey);
    }

    public void deleteRedirection(String secretKey) {
        repo.delete(findBySecretKey(secretKey));
    }
}