package com.icnmicros.app.oauth.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.icnmicros.app.commons.usuarios.models.entity.Usuario;
import com.icnmicros.app.oauth.services.IUsuarioService;

import feign.FeignException;

@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher {

	private Logger log = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);

	@Autowired
	private IUsuarioService usuarioService;

	@Override
	public void publishAuthenticationSuccess(Authentication authentication) {
		if (authentication.getName().equalsIgnoreCase("frontendapp")) {
			return; // si es igual a frontendapp se salen del método!
		}

		UserDetails user = (UserDetails) authentication.getPrincipal();
		String mensaje = "Success Login: " + user.getUsername();
		System.out.println(mensaje);
		log.info(mensaje);

		Usuario usuario = usuarioService.findByUsername(authentication.getName());

	}

	@Override
	public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
		String mensaje = "Error en login: " + exception.getMessage();
		System.out.println(mensaje);
		log.error(mensaje);

		StringBuilder error = new StringBuilder();
		error.append(mensaje);

		try {
			Usuario usuario = usuarioService.findByUsername(authentication.getName());
			if (usuario.getIntentos() == null) {
				usuario.setIntentos(0);
			}

			log.info("Intento actual corresponde: " + usuario.getIntentos());
			usuario.setIntentos(usuario.getIntentos() + 1);
			log.info("Intentos del usuario: " + usuario.getIntentos());

			error.append("Intentos del usuario: " + usuario.getIntentos());
			
			if (usuario.getIntentos() >= 3) {
				log.error(String.format("El usuario %s fue desabilitado por superar 3 intentos", usuario.getNombre()));
				usuario.setEnabled(false);
			}

			usuarioService.update(usuario, usuario.getId());

		} catch (FeignException e) {
			log.error(String.format("El usuario %s no existe en el sistema: ", authentication.getName()));
		}
	}

}
