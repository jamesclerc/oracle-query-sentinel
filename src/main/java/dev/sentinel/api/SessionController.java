package dev.sentinel.api;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.LockInfo;
import dev.sentinel.repository.VLockRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "sessions")
public class SessionController {

    private final VLockRepository vLock;
    private final SentinelProperties props;

    public SessionController(VLockRepository vLock, SentinelProperties props) {
        this.vLock = vLock;
        this.props = props;
    }

    @GetMapping("/locks")
    public List<LockInfo> locks() {
        return vLock.contendedLocks(props.rules().lockWaitThresholdSeconds());
    }
}
