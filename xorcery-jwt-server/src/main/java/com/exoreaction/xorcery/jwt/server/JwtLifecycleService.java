package com.exoreaction.xorcery.jwt.server;

import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jwt.server")
@RunLevel(18)
public class JwtLifecycleService {
    private final JwtService jwtService;

    @Inject
    public JwtLifecycleService(JwtService jwtService) {
        this.jwtService = jwtService;
    }


}
