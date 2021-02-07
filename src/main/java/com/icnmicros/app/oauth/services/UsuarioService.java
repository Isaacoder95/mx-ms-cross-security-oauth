package com.icnmicros.app.oauth.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.icnmicros.app.commons.usuarios.models.entity.Usuario;
import com.icnmicros.app.oauth.clients.UsuarioFeingClient;

import brave.Tracer;
import feign.FeignException;

@Service
public class UsuarioService implements UserDetailsService, IUsuarioService {

	private Logger log = LoggerFactory.getLogger(UsuarioService.class);

	@Autowired
	private UsuarioFeingClient client;

	@Autowired
	private Tracer tracer;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		try {

			Usuario usuario = client.findByUsername(username);

			List<GrantedAuthority> authorities = usuario.getRoles().stream()
					.map(role -> new SimpleGrantedAuthority(role.getNombre()))
					.peek(authority -> log.info("Role: " + authority.getAuthority())).collect(Collectors.toList());

			log.info("Usuario autenticado: " + username);
			System.out.println("Usuario autenticado: " + username);

			return new User(username, usuario.getPassword(), usuario.getEnabled(), true, true, true, authorities);
		} catch (FeignException e) {
			log.error("Error en el login, no existe el usuario '" + username + "' en el sistema ");
			System.out.println("Error en el login, no existe el usuario '" + username + "' en el sistema ");

			String error = "Error en el login, no existe el usuario '" + username + "' en el sistema ";
			tracer.currentSpan().tag("error mensaje", error + ": " + e.getMessage());
			throw new UsernameNotFoundException(
					"Error en el login, no existe el usuario '" + username + "' en el sistema ");
		}
	}

	@Override
	public Usuario findByUsername(String username) {
		return client.findByUsername(username);
	}

	@Override
	public Usuario update(Usuario usuario, Long id) {
		return client.update(usuario, id);
	}

}
