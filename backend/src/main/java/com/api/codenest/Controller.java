package com.api.codenest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import javax.tools.SimpleJavaFileObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api/code")
public class Controller {

	@Autowired
	private CodeExecutorService codeSummaryService;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostMapping("/execute")
	public ResponseEntity<String> executeCode(@RequestParam String language, @RequestParam String code,
											  @RequestParam String input) {

		System.out.println("Execute code called for language " + language);
		String id = generateRandomString(5);

		// Save the language and code into MySQL
		try {
			String sql = "INSERT INTO codesummary (id, language, code, input) VALUES (?, ?, ?, ?)";
			jdbcTemplate.update(sql, id, language, code, input);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving code to database");
		}

		CompletableFuture.runAsync(() -> {
			switch (language.toLowerCase()) {
				case "java":
					executeJavaCode(id);
					break;
				case "python":
					executePythonCode(id);
					break;
				case "cpp":
					executeCppCode(id);
					break;
				case "javascript":
					executeJavaScriptCode(id);
					break;
				// Add cases for other languages as needed
				default:
					System.err.println("Unsupported language: " + language);
			}
		});

		return ResponseEntity.ok(id);
	}

	@CrossOrigin(origins = "https://onlinecodingplateform.netlify.app/")
	@GetMapping("/output")
	public ResponseEntity<String> getOutput(@RequestParam String id) {
		Optional<CodeSummaryEntity> entity = codeSummaryService.getFieldById(id);
		if (entity.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		String output = entity.get().getOutput();
		if (output == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(output);
	}

	static class StringSourceJavaObject extends SimpleJavaFileObject {
		private final String code;
		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		public StringSourceJavaObject(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}

		public String getOutput() {
			return outputStream.toString();
		}

		@Override
		public OutputStream openOutputStream() {
			return outputStream;
		}
	}

//	private void executeJavaCode(String id) {
//		String output;
//		try {
//			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
//			String input = res.get().getInput();
//			String code = res.get().getCode();
//
//			// Create temporary file for the Java code
//			Path sourcePath = Files.createTempFile("Main", ".java");
//			Files.write(sourcePath, code.getBytes());
//
//			// Compile the Java code
//			Process compileProcess = new ProcessBuilder("javac", sourcePath.toString()).start();
//			compileProcess.waitFor();
//
//			// Execute the compiled Java code with input
//			Path classPath = Files.createTempFile("Main", ".class");
//			ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", classPath.getParent().toString(), "Main");
//			Process executeProcess = processBuilder.start();
//			try (OutputStream outputStream = executeProcess.getOutputStream()) {
//				outputStream.write(input.getBytes());
//			}
//
//			BufferedReader reader = new BufferedReader(new InputStreamReader(executeProcess.getInputStream()));
//			StringBuilder result = new StringBuilder();
//			String line;
//			while ((line = reader.readLine()) != null) {
//				result.append(line).append("\n");
//			}
//
//			// Wait for the process to complete and capture any errors
//			int exitCode = executeProcess.waitFor();
//			if (exitCode == 0) {
//				output = result.toString();
//			} else {
//				BufferedReader errorReader = new BufferedReader(new InputStreamReader(executeProcess.getErrorStream()));
//				StringBuilder errorResult = new StringBuilder();
//				String errorLine;
//				while ((errorLine = errorReader.readLine()) != null) {
//					errorResult.append(errorLine).append("\n");
//				}
//				output = "Error executing Java code:\n" + errorResult.toString();
//			}
//		} catch (Exception e) {
//			output = "Error executing Java code:\n" + e.toString();
//		}
//		codeSummaryService.updateFieldById(id, output);
//	}
//
//	private void executePythonCode(String id) {
//		String output;
//		try {
//			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
//			String input = res.get().getInput();
//			String code = res.get().getCode();
//
//			Path sourcePath = Files.createTempFile("main", ".py");
//			Files.write(sourcePath, code.getBytes());
//
//			ProcessBuilder processBuilder = new ProcessBuilder("python", sourcePath.toString());
//			Process process = processBuilder.start();
//			try (OutputStream outputStream = process.getOutputStream()) {
//				outputStream.write(input.getBytes());
//			}
//
//			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//			StringBuilder result = new StringBuilder();
//			String line;
//			while ((line = reader.readLine()) != null) {
//				result.append(line).append("\n");
//			}
//
//			int exitCode = process.waitFor();
//			if (exitCode == 0) {
//				output = result.toString();
//			} else {
//				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//				StringBuilder errorResult = new StringBuilder();
//				String errorLine;
//				while ((errorLine = errorReader.readLine()) != null) {
//					errorResult.append(errorLine).append("\n");
//				}
//				output = "Error executing Python code:\n" + errorResult.toString();
//			}
//		} catch (Exception e) {
//			output = "Error executing Python code:\n" + e.toString();
//		}
//		codeSummaryService.updateFieldById(id, output);
//	}
//
//	private void executeCppCode(String id) {
//		String output;
//		try {
//			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
//			String input = res.get().getInput();
//			String code = res.get().getCode();
//
//			Path sourcePath = Files.createTempFile("main", ".cpp");
//			Files.write(sourcePath, code.getBytes());
//
//			// Compile the C++ code
//			Process compileProcess = new ProcessBuilder("g++", sourcePath.toString(), "-o", "main").start();
//			compileProcess.waitFor();
//
//			// Execute the compiled C++ code with input
//			ProcessBuilder processBuilder = new ProcessBuilder("./main");
//			Process executeProcess = processBuilder.start();
//			try (OutputStream outputStream = executeProcess.getOutputStream()) {
//				outputStream.write(input.getBytes());
//			}
//
//			BufferedReader reader = new BufferedReader(new InputStreamReader(executeProcess.getInputStream()));
//			StringBuilder result = new StringBuilder();
//			String line;
//			while ((line = reader.readLine()) != null) {
//				result.append(line).append("\n");
//			}
//
//			int exitCode = executeProcess.waitFor();
//			if (exitCode == 0) {
//				output = result.toString();
//			} else {
//				BufferedReader errorReader = new BufferedReader(new InputStreamReader(executeProcess.getErrorStream()));
//				StringBuilder errorResult = new StringBuilder();
//				String errorLine;
//				while ((errorLine = errorReader.readLine()) != null) {
//					errorResult.append(errorLine).append("\n");
//				}
//				output = "Error executing C++ code:\n" + errorResult.toString();
//			}
//		} catch (Exception e) {
//			output = "Error executing C++ code:\n" + e.toString();
//		}
//		codeSummaryService.updateFieldById(id, output);
//	}
//
//	private void executeJavaScriptCode(String id) {
//		String output;
//		try {
//			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
//			String input = res.get().getInput();
//			String code = res.get().getCode();
//
//			Path scriptPath = Files.createTempFile("main", ".js");
//			Files.write(scriptPath, code.getBytes());
//
//			ProcessBuilder processBuilder = new ProcessBuilder("node", scriptPath.toString());
//			Process process = processBuilder.start();
//			try (OutputStream outputStream = process.getOutputStream()) {
//				outputStream.write(input.getBytes());
//			}
//
//			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//			StringBuilder result = new StringBuilder();
//			String line;
//			while ((line = reader.readLine()) != null) {
//				result.append(line).append("\n");
//			}
//			output = result.toString();
//
//			int exitCode = process.waitFor();
//			if (exitCode != 0) {
//				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//				StringBuilder errorResult = new StringBuilder();
//				String errorLine;
//				while ((errorLine = errorReader.readLine()) != null) {
//					errorResult.append(errorLine).append("\n");
//				}
//				output = "Error executing JavaScript code:\n" + errorResult.toString();
//			}
//		} catch (Exception e) {
//			output = "Error executing JavaScript code:\n" + e.toString();
//		}
//		codeSummaryService.updateFieldById(id, output);
//	}

	//new code
	private void executeJavaCode(String id) {
		String output;
		try {
			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
			String input = res.get().getInput();
			String code = res.get().getCode();

			Path sourcePath = Files.createTempFile("Main", ".java");
			Files.write(sourcePath, code.getBytes());

			// Create Docker command to compile and run Java code
			String dockerCommand = String.format(
					"docker run --rm -v %s:/app -w /app openjdk:17-jdk-slim /bin/sh -c 'javac Main.java && java Main'",
					sourcePath.getParent().toAbsolutePath()
			);

			ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", dockerCommand);
			Process process = processBuilder.start();
			try (OutputStream outputStream = process.getOutputStream()) {
				outputStream.write(input.getBytes());
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line).append("\n");
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				output = result.toString();
			} else {
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				StringBuilder errorResult = new StringBuilder();
				String errorLine;
				while ((errorLine = errorReader.readLine()) != null) {
					errorResult.append(errorLine).append("\n");
				}
				output = "Error executing Java code:\n" + errorResult.toString();
			}
		} catch (Exception e) {
			output = "Error executing Java code:\n" + e.toString();
		}
		codeSummaryService.updateFieldById(id, output);
	}

	private void executePythonCode(String id) {
		String output;
		try {
			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
			String input = res.get().getInput();
			String code = res.get().getCode();

			Path sourcePath = Files.createTempFile("main", ".py");
			Files.write(sourcePath, code.getBytes());

			// Create Docker command to run Python code
			String dockerCommand = String.format(
					"docker run --rm -v %s:/app -w /app python:3 python main.py",
					sourcePath.getParent().toAbsolutePath()
			);

			ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", dockerCommand);
			Process process = processBuilder.start();
			try (OutputStream outputStream = process.getOutputStream()) {
				outputStream.write(input.getBytes());
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line).append("\n");
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				output = result.toString();
			} else {
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				StringBuilder errorResult = new StringBuilder();
				String errorLine;
				while ((errorLine = errorReader.readLine()) != null) {
					errorResult.append(errorLine).append("\n");
				}
				output = "Error executing Python code:\n" + errorResult.toString();
			}
		} catch (Exception e) {
			output = "Error executing Python code:\n" + e.toString();
		}
		codeSummaryService.updateFieldById(id, output);
	}

	private void executeCppCode(String id) {
		String output;
		try {
			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
			String input = res.get().getInput();
			String code = res.get().getCode();

			Path sourcePath = Files.createTempFile("main", ".cpp");
			Files.write(sourcePath, code.getBytes());

			// Create Docker command to compile and run C++ code
			String dockerCommand = String.format(
					"docker run --rm -v %s:/app -w /app gcc:latest /bin/sh -c 'g++ main.cpp -o main && ./main'",
					sourcePath.getParent().toAbsolutePath()
			);

			ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", dockerCommand);
			Process process = processBuilder.start();
			try (OutputStream outputStream = process.getOutputStream()) {
				outputStream.write(input.getBytes());
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line).append("\n");
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				output = result.toString();
			} else {
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				StringBuilder errorResult = new StringBuilder();
				String errorLine;
				while ((errorLine = errorReader.readLine()) != null) {
					errorResult.append(errorLine).append("\n");
				}
				output = "Error executing C++ code:\n" + errorResult.toString();
			}
		} catch (Exception e) {
			output = "Error executing C++ code:\n" + e.toString();
		}
		codeSummaryService.updateFieldById(id, output);
	}

	private void executeJavaScriptCode(String id) {
		String output;
		try {
			Optional<CodeSummaryEntity> res = codeSummaryService.getFieldById(id);
			String input = res.get().getInput();
			String code = res.get().getCode();

			Path scriptPath = Files.createTempFile("main", ".js");
			Files.write(scriptPath, code.getBytes());

			// Create Docker command to run JavaScript code
			String dockerCommand = String.format(
					"docker run --rm -v %s:/app -w /app node:18 node main.js",
					scriptPath.getParent().toAbsolutePath()
			);

			ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", dockerCommand);
			Process process = processBuilder.start();
			try (OutputStream outputStream = process.getOutputStream()) {
				outputStream.write(input.getBytes());
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line).append("\n");
			}
			output = result.toString();

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				StringBuilder errorResult = new StringBuilder();
				String errorLine;
				while ((errorLine = errorReader.readLine()) != null) {
					errorResult.append(errorLine).append("\n");
				}
				output = "Error executing JavaScript code:\n" + errorResult.toString();
			}
		} catch (Exception e) {
			output = "Error executing JavaScript code:\n" + e.toString();
		}
		codeSummaryService.updateFieldById(id, output);
	}

	public static String generateRandomString(int length) {
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(CHARACTERS.length());
			char randomChar = CHARACTERS.charAt(randomIndex);
			sb.append(randomChar);
		}

		return sb.toString();
	}
}
