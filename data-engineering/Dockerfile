# syntax=docker/dockerfile:1

# ---------- Stage 1: build dependencies ----------
FROM python:3.11-slim AS build
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
WORKDIR /app

# Build into an isolated virtualenv we can copy whole into the runtime image.
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Dependencies first — cached unless requirements.txt changes.
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# ---------- Stage 2: runtime ----------
FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH"
WORKDIR /app

# Dedicated non-root user/group.
RUN groupadd --system app && useradd --system --gid app --no-create-home app

# Bring over the prebuilt venv, then the application code.
COPY --from=build /opt/venv /opt/venv
COPY --chown=app:app . .

USER app
CMD ["python", "etl_pipeline.py"]
