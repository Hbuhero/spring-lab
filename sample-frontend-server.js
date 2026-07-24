const http = require("node:http");

const PORT = 3000;
const EXCHANGE_API_URL = "http://localhost:8080/api/v1/auth/exchange";

/**
 * Escape values before inserting them into HTML.
 */
function escapeHtml(value) {
    if (value === null || value === undefined || value === "") {
        return "—";
    }

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function renderField(label, value, options = {}) {
    const { fullWidth = false, monospace = false } = options;

    return `
    <div class="field ${fullWidth ? "full-width" : ""}">
      <span class="field-label">${escapeHtml(label)}</span>
      <div class="field-value ${monospace ? "monospace" : ""}">
        ${escapeHtml(value)}
      </div>
    </div>
  `;
}

function renderLoginResponse(loginResponse) {
    const user = loginResponse.user ?? {};

    return `
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1.0"
        />
        <title>OAuth Login Response</title>

        <style>
          * {
            box-sizing: border-box;
          }

          body {
            margin: 0;
            padding: 40px 20px;
            min-height: 100vh;
            font-family:
              Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
              sans-serif;
            background: #f3f4f6;
            color: #111827;
          }

          .container {
            width: 100%;
            max-width: 960px;
            margin: 0 auto;
          }

          .header {
            margin-bottom: 24px;
          }

          .header h1 {
            margin: 0 0 8px;
            font-size: 30px;
          }

          .header p {
            margin: 0;
            color: #6b7280;
          }

          .success-message {
            margin-bottom: 24px;
            padding: 14px 18px;
            border: 1px solid #a7f3d0;
            border-radius: 10px;
            background: #ecfdf5;
            color: #065f46;
          }

          .card {
            margin-bottom: 24px;
            overflow: hidden;
            border: 1px solid #e5e7eb;
            border-radius: 14px;
            background: #ffffff;
            box-shadow: 0 4px 12px rgb(0 0 0 / 5%);
          }

          .card-header {
            padding: 18px 22px;
            border-bottom: 1px solid #e5e7eb;
            background: #f9fafb;
          }

          .card-header h2 {
            margin: 0;
            font-size: 18px;
          }

          .card-content {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 18px;
            padding: 22px;
          }

          .field {
            min-width: 0;
          }

          .full-width {
            grid-column: 1 / -1;
          }

          .field-label {
            display: block;
            margin-bottom: 7px;
            color: #6b7280;
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 0.06em;
            text-transform: uppercase;
          }

          .field-value {
            min-height: 42px;
            padding: 11px 13px;
            overflow-wrap: anywhere;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            background: #f9fafb;
            line-height: 1.5;
          }

          .monospace {
            font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
            font-size: 13px;
          }

          details {
            margin-bottom: 24px;
            border: 1px solid #e5e7eb;
            border-radius: 14px;
            background: #ffffff;
          }

          summary {
            padding: 18px 22px;
            cursor: pointer;
            font-weight: 700;
          }

          pre {
            margin: 0;
            padding: 22px;
            overflow-x: auto;
            border-top: 1px solid #e5e7eb;
            background: #111827;
            color: #e5e7eb;
            font-size: 13px;
            line-height: 1.6;
          }

          @media (max-width: 650px) {
            body {
              padding: 24px 14px;
            }

            .card-content {
              grid-template-columns: 1fr;
            }

            .full-width {
              grid-column: auto;
            }
          }
        </style>
      </head>

      <body>
        <main class="container">
          <header class="header">
            <h1>OAuth Login Response</h1>
            <p>The authorization code was exchanged successfully.</p>
          </header>

          <div class="success-message">
            Authentication completed successfully.
          </div>

          <section class="card">
            <div class="card-header">
              <h2>User information</h2>
            </div>

            <div class="card-content">
              ${renderField("User ID", user.id)}
              ${renderField("Status", user.status)}
              ${renderField("Name", user.name)}
              ${renderField("Email", user.email)}
              ${renderField("Phone number", user.phoneNumber)}
              ${renderField("Role", user.role)}
              ${renderField("Permissions", user.permissions, {
        fullWidth: true,
    })}
              ${renderField(
        "Password reset required",
        user.passwordReset === true
            ? "Yes"
            : user.passwordReset === false
                ? "No"
                : null,
    )}
              ${renderField("Created at", user.createdAt)}
            </div>
          </section>

          <section class="card">
            <div class="card-header">
              <h2>Authentication information</h2>
            </div>

            <div class="card-content">
              ${renderField(
        "Token type",
        loginResponse.tokenType ?? "Bearer",
    )}
              ${renderField(
        "Token expiration",
        loginResponse.tokenExpiration,
    )}
              ${renderField("Access token", loginResponse.token, {
        fullWidth: true,
        monospace: true,
    })}
              ${renderField("Refresh token", loginResponse.refreshToken, {
        fullWidth: true,
        monospace: true,
    })}
            </div>
          </section>

          <details>
            <summary>View raw JSON response</summary>
            <pre>${escapeHtml(JSON.stringify(loginResponse, null, 2))}</pre>
          </details>
        </main>
      </body>
    </html>
  `;
}

function renderMessagePage(title, message, statusCode) {
    return `
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1.0"
        />
        <title>${escapeHtml(title)}</title>

        <style>
          body {
            display: grid;
            place-items: center;
            min-height: 100vh;
            margin: 0;
            padding: 20px;
            font-family:
              Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
              sans-serif;
            background: #f3f4f6;
            color: #111827;
          }

          .message {
            width: 100%;
            max-width: 620px;
            padding: 30px;
            border: 1px solid #e5e7eb;
            border-radius: 14px;
            background: #ffffff;
            box-shadow: 0 4px 12px rgb(0 0 0 / 6%);
          }

          h1 {
            margin-top: 0;
          }

          p {
            color: #4b5563;
            line-height: 1.6;
          }

          code {
            padding: 3px 6px;
            border-radius: 5px;
            background: #f3f4f6;
          }

          .status {
            color: #6b7280;
            font-size: 13px;
          }
        </style>
      </head>

      <body>
        <main class="message">
          <h1>${escapeHtml(title)}</h1>
          <p>${escapeHtml(message)}</p>
          <div class="status">HTTP status: ${statusCode}</div>
        </main>
      </body>
    </html>
  `;
}

const server = http.createServer(async (request, response) => {
    try {
        const requestUrl = new URL(
            request.url,
            `http://${request.headers.host ?? `localhost:${PORT}`}`,
        );

        if (
            request.method !== "GET" ||
            requestUrl.pathname !== "/oauth/callback"
        ) {
            response.writeHead(404, {
                "Content-Type": "text/html; charset=utf-8",
            });

            response.end(
                renderMessagePage(
                    "Page not found",
                    "Use the OAuth callback endpoint at /oauth/callback?code=YOUR_CODE.",
                    404,
                ),
            );

            return;
        }

        const code = requestUrl.searchParams.get("code");

        if (!code) {
            response.writeHead(400, {
                "Content-Type": "text/html; charset=utf-8",
            });

            response.end(
                renderMessagePage(
                    "Missing authorization code",
                    "The query parameter 'code' is required.",
                    400,
                ),
            );

            return;
        }

        const exchangeUrl = new URL(EXCHANGE_API_URL);
        exchangeUrl.searchParams.set("code", code);

        const exchangeResponse = await fetch(exchangeUrl, {
            method: "POST",
            headers: {
                Accept: "application/json",
            },
            signal: AbortSignal.timeout(15_000),
        });

        const responseText = await exchangeResponse.text();

        let responseBody;

        try {
            responseBody = JSON.parse(responseText);
        } catch {
            responseBody = null;
        }

        if (!exchangeResponse.ok) {
            const errorMessage =
                responseBody?.message ??
                responseBody?.error ??
                responseText ??
                "The token exchange API returned an error.";

            response.writeHead(exchangeResponse.status, {
                "Content-Type": "text/html; charset=utf-8",
            });

            response.end(
                renderMessagePage(
                    "Login exchange failed",
                    errorMessage,
                    exchangeResponse.status,
                ),
            );

            return;
        }

        if (!responseBody) {
            response.writeHead(502, {
                "Content-Type": "text/html; charset=utf-8",
            });

            response.end(
                renderMessagePage(
                    "Invalid API response",
                    "The exchange API did not return valid JSON.",
                    502,
                ),
            );

            return;
        }

        response.writeHead(200, {
            "Content-Type": "text/html; charset=utf-8",
            "Cache-Control": "no-store",
        });

        response.end(renderLoginResponse(responseBody));
    } catch (error) {
        console.error("OAuth callback error:", error);

        const message =
            error.name === "TimeoutError"
                ? "The exchange API request timed out."
                : "Could not connect to the exchange API at localhost:8080.";

        response.writeHead(502, {
            "Content-Type": "text/html; charset=utf-8",
        });

        response.end(renderMessagePage("Server error", message, 502));
    }
});

server.listen(PORT, () => {
    console.log(`OAuth callback server running at http://localhost:${PORT}`);
    console.log(
        `Callback URL: http://localhost:${PORT}/oauth/callback?code=YOUR_CODE`,
    );
});