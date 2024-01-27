from typing import Optional, Iterable
import sys
from socketserver import StreamRequestHandler, TCPServer
import uuid


def log_segments(info: str, segments: Iterable[str]):
    print(f"{info}:")
    for segment in segments:
        print(segment)


class MllpHandler(StreamRequestHandler):
    def handle(self) -> None:
        message = self.receive_message()
        
        if message is not None:
            segments = message.split("\r")

            log_segments("Received", segments)

            if len(segments) == 0:
                raise RuntimeError("Message has no segments")
            
            msh = segments[0]
            if len(msh) < 3 or msh[:3] != "MSH":
                raise RuntimeError("First segment is not MSH")
            
            if len(msh) <= 4:
                raise RuntimeError("MSH is missing field separator")
            
            separator = msh[3]

            fields = msh.split(separator)
            if len(fields) < 12:
                raise RuntimeError("MSH has less than 12 fields")
            
            encoding_characters = fields[1]
            sending_application = fields[2]
            sending_facility = fields[3]
            receiving_application = fields[4]
            receiving_facility = fields[5]
            message_control_id = fields[9]
            processing_id = fields[10]
            version_id = fields[11]

            response_msh = separator.join([
                "MSH",
                encoding_characters,
                receiving_application,
                receiving_facility,
                sending_application,
                sending_facility,
                "",
                "",
                "ACK",
                str(uuid.uuid4()),
                processing_id,
                version_id
            ])

            response_msa = separator.join(["MSA", "AA", message_control_id])

            log_segments("Sending", [response_msh, response_msa])

            self.send_message(f"{response_msh}\r{response_msa}\r")

    def receive_message(self) -> Optional[str]:
        data = bytearray()

        while len(data) < 2 or data[-2] != "\x1c" or data[-1] != "\x0d":
            data_read = self.rfile.read(1024)
            
            if len(data_read) == 0:
                if len(data) > 0:
                    raise IOError("EOF without end block character and carriage return sent")
                break
            
            if len(data) == 0 and data_read[0] != "\x0b":
                raise IOError("No start block character sent")

            data.extend(data_read)
        
        return None if len(data) == 0 else data[1:-2].decode()
    
    def send_message(self, message: str):
        self.wfile.write(b"\x0b")
        self.wfile.write(message.encode())
        self.wfile.write(b"\x1c")
        self.wfile.write(b"\x0d")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Missing port parameter!")
        sys.exit()

    port = int(sys.argv[1])

    with TCPServer(("localhost", port), MllpHandler) as server:
        server.serve_forever()
