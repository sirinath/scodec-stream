package scodec

import scalaz.stream.{ Process, async }
import scalaz.concurrent.{ Strategy, Task }

import scodec.bits.BitVector

package object stream {
  type StreamDecoder[+A] = scodec.stream.decode.StreamDecoder[A]
  val StreamDecoder = scodec.stream.decode.StreamDecoder

  type StreamEncoder[-A] = scodec.stream.encode.StreamEncoder[A]
  val StreamEncoder = scodec.stream.encode.StreamEncoder

  type StreamCodec[A] = scodec.stream.codec.StreamCodec[A]
  val StreamCodec = scodec.stream.codec.StreamCodec

  /** Constructs a lazy `BitVector` by continuously reading from the supplied process until it halts. */
  def toLazyBitVector(p: Process[Task, BitVector], bufferSize: Int = 100)(implicit S: Strategy): BitVector = {
    val queue = async.boundedQueue[BitVector](bufferSize)(S)
    p.to(queue.enqueue).onHalt { cause => Process.eval_(queue.close) }.run.runAsync(_ => ())
    BitVector.unfold(()) { _ => queue.dequeue.take(1).runLast.run.map { b => (b, ()) } }
  }
}
