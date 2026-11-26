package br.com.financas.backend.usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class UsuarioSecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    UserDetailsService usuariosParaAutenticacao(UsuarioRepository repository) {
        return email -> repository.buscarPorEmail(email.strip()).map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException(null));
    }

    @Bean
    SecurityFilterChain segurancaUsuarios(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver erros) throws Exception {
        var cadastro = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/usuarios");
        var tokensCsrf = new CookieCsrfTokenRepository();
        tokensCsrf.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
        AuthenticationEntryPoint autenticacaoObrigatoria = (requisicao, resposta, erro) -> {
            resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"usuarios\", charset=\"UTF-8\"");
            responderErro(erros, requisicao, resposta, HttpStatus.UNAUTHORIZED, erro);
        };

        return http.securityMatcher("/api/usuarios", "/api/usuarios/**")
                .authorizeHttpRequests(acesso -> acesso.requestMatchers(cadastro).permitAll().anyRequest().authenticated())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(tokensCsrf).ignoringRequestMatchers(cadastro))
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(autenticacaoObrigatoria)
                        .accessDeniedHandler((requisicao, resposta, erro) ->
                                responderErro(erros, requisicao, resposta, HttpStatus.FORBIDDEN, erro)))
                .httpBasic(basic -> basic.authenticationEntryPoint(autenticacaoObrigatoria))
                .build();
    }

    @Bean
    SecurityFilterChain segurancaSessao(HttpSecurity http, UsuarioRepository usuarios,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver erros) throws Exception {
        var caminhos = PathPatternRequestMatcher.withDefaults();
        var sair = caminhos.matcher(HttpMethod.DELETE, "/api/sessao");
        var categorias = caminhos.matcher("/api/categorias/**");
        var invalidarSessao = new SecurityContextLogoutHandler();
        AuthenticationEntryPoint autenticacaoObrigatoria = (requisicao, resposta, erro) -> {
            if (erro instanceof CredentialsExpiredException) {
                invalidarSessao.logout(requisicao, resposta, SecurityContextHolder.getContext().getAuthentication());
            }
            responderErro(erros, requisicao, resposta, HttpStatus.UNAUTHORIZED, erro);
        };

        return http.securityMatcher("/api/sessao/**", "/api/categorias/**")
                .authorizeHttpRequests(acesso -> acesso.requestMatchers(HttpMethod.GET, "/api/sessao/csrf")
                        .permitAll().requestMatchers(categorias).access((autenticacao, contexto) -> {
                            if (!(autenticacao.get().getPrincipal() instanceof UsuarioAutenticado usuario)) {
                                return new AuthorizationDecision(false);
                            }
                            if (!usuarios.existsById(usuario.id())) {
                                throw new CredentialsExpiredException(null);
                            }
                            return new AuthorizationDecision(true);
                        }).anyRequest().authenticated())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/api/sessao")
                        .usernameParameter("email").passwordParameter("senha")
                        .successHandler((requisicao, resposta, autenticacao) ->
                                resposta.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .failureHandler(autenticacaoObrigatoria::commence))
                .logout(logout -> logout.logoutRequestMatcher(sair).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(autenticacaoObrigatoria)
                        .accessDeniedHandler((requisicao, resposta, erro) -> {
                            var autenticacao = SecurityContextHolder.getContext().getAuthentication();
                            if (categorias.matches(requisicao)
                                    && (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado))) {
                                autenticacaoObrigatoria.commence(requisicao, resposta, new InsufficientAuthenticationException(null));
                            } else {
                                responderErro(erros, requisicao, resposta, HttpStatus.FORBIDDEN, erro);
                            }
                        }))
                .build();
    }

    private void responderErro(HandlerExceptionResolver erros, HttpServletRequest requisicao,
            HttpServletResponse resposta, HttpStatus status, Exception erro) {
        resposta.setStatus(status.value());
        erros.resolveException(requisicao, resposta, null, erro);
    }
}
